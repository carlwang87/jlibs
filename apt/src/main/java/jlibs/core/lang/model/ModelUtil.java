/**
 * Copyright 2015 The JLibs Project
 *
 * The JLibs Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package jlibs.core.lang.model;

import jlibs.core.annotation.processing.AnnotationError;
import jlibs.core.annotation.processing.Environment;

import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.StandardLocation;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Santhosh Kumar T
 */
public class ModelUtil{
    @SuppressWarnings({"unchecked"})
    public static <T> T parent(Element element, Class<T> type){
        do{
            element = element.getEnclosingElement();
        }while(!type.isInstance(element));
        return (T)element;
    }

    public static TypeElement getSuper(TypeElement clazz){
        TypeMirror superMirror = clazz.getSuperclass();
        if(superMirror instanceof DeclaredType)
            return (TypeElement)((DeclaredType)superMirror).asElement();
        else
            return null;
    }

    public static boolean isAssignable(TypeMirror type, Class clazz){
        TypeMirror superType = Environment.get().getElementUtils().getTypeElement(clazz.getCanonicalName()).asType();
        return Environment.get().getTypeUtils().isAssignable(type, superType);
    }

    public static String getPackage(Element elem){
        while(!(elem instanceof PackageElement))
            elem = elem.getEnclosingElement();
        return ((PackageElement)elem).getQualifiedName().toString();
    }

    public static boolean isInnerClass(TypeElement elem){
        return !(elem.getEnclosingElement() instanceof PackageElement);
    }

    public static boolean isPrimitive(TypeMirror mirror){
        switch(mirror.getKind()){
            case ARRAY:
            case DECLARED:
                return false;
            default:
                return true;
        }
    }

    private static Map<String, String> primitiveWrappers = new HashMap<String, String>();
    static{
        primitiveWrappers.put(Boolean.class.getName(), boolean.class.getName());
        primitiveWrappers.put(Byte.class.getName(), byte.class.getName());
        primitiveWrappers.put(Short.class.getName(), short.class.getName());
        primitiveWrappers.put(Integer.class.getName(), int.class.getName());
        primitiveWrappers.put(Long.class.getName(), long.class.getName());
        primitiveWrappers.put(Character.class.getName(), char.class.getName());
        primitiveWrappers.put(Float.class.getName(), float.class.getName());
        primitiveWrappers.put(Double.class.getName(), double.class.getName());

    }

    public static boolean isPrimitiveWrapper(TypeMirror mirror){
        return mirror.getKind()==TypeKind.DECLARED && primitiveWrappers.containsKey(toString(mirror, false));
    }

    public static String getPrimitive(String primitiveWrapper){
        return primitiveWrappers.get(primitiveWrapper);
    }

    public static String toString(TypeMirror mirror, boolean usePrimitiveWrappers){
        TypeKind kind = mirror.getKind();
        switch(kind){
            case VOID:
                return "void";
            case DECLARED:
                Name paramType = ((TypeElement)((DeclaredType)mirror).asElement()).getQualifiedName();

                List<? extends TypeMirror> typeArguments = ((DeclaredType)mirror).getTypeArguments();
                if(typeArguments.size()==0)
                    return paramType.toString();
                else{
                    StringBuilder buff = new StringBuilder(paramType).append('<');
                    for(TypeMirror typeArgument: typeArguments)
                        buff.append(toString(typeArgument, false));
                    return buff.append('>').toString();
                }
            case INT:
                return usePrimitiveWrappers ? Integer.class.getName() : kind.toString().toLowerCase();
            case CHAR:
                return usePrimitiveWrappers ? Character.class.getName() : kind.toString().toLowerCase();
            case BOOLEAN:
            case FLOAT:
            case DOUBLE:
            case LONG:
            case SHORT:
            case BYTE:
                String name = kind.toString().toLowerCase();
                if(usePrimitiveWrappers)
                    return "java.lang."+Character.toUpperCase(name.charAt(0))+name.substring(1);
                else
                    return name;
            case ARRAY:
                return toString(((ArrayType)mirror).getComponentType(), false)+"[]";
            default:
                throw new RuntimeException(kind +" is not implemented for "+mirror.getClass());
        }
    }

    public static Modifier getModifier(Set<Modifier> set, Modifier... modifiers){
        for(Modifier modifier: modifiers){
            if(set.contains(modifier))
                return modifier;
        }
        return null;
    }

    public static boolean isAccessible(Element element, boolean samePackage, boolean subClass){
        Modifier modifier = getModifier(element.getModifiers(), Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE);
        if(modifier==null){
            if(!samePackage)
                return false;
        }else{
            switch(modifier){
                case PRIVATE:
                    return false;
                case PROTECTED:
                    if(!samePackage && !subClass)
                        return false;
            }
        }
        return true;
    }

    public static String signature(ExecutableElement method, boolean useParamNames){
        StringBuilder buff = new StringBuilder();

        Set<Modifier> modifiers = method.getModifiers();
        Modifier modifier = getModifier(modifiers, Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE);
        if(modifier!=null)
            buff.append(modifier).append(' ');

        buff.append(toString(method.getReturnType(), false));
        buff.append(' ');
        buff.append(method.getSimpleName());
        buff.append('(');
        int i = 0;
        for(VariableElement param : method.getParameters()){
            if(i>0)
                buff.append(", ");
            buff.append(toString(param.asType(), false));
            if(useParamNames)
                buff.append(' ').append(param.getSimpleName());
            i++;
        }
        buff.append(')');

        List<? extends TypeMirror> throwTypes = method.getThrownTypes();
        if(throwTypes.size()>0){
            buff.append(" throws ");
            i = 0;
            for(TypeMirror throwType: throwTypes){
                if(i>0)
                    buff.append(", ");
                buff.append(throwType);
                i++;
            }
        }

        return buff.toString();
    }

    public static VariableElement getParameter(ExecutableElement method, String paramName){
        for(VariableElement param : method.getParameters()){
            if(param.getSimpleName().contentEquals(paramName))
                return param;
        }
        return null;
    }
    
    /*-------------------------------------------------[ Annotation ]---------------------------------------------------*/

    public static boolean matches(AnnotationMirror mirror, Class annotation){
        return ((TypeElement)mirror.getAnnotationType().asElement()).getQualifiedName().contentEquals(annotation.getCanonicalName());
    }

    public static AnnotationMirror getAnnotationMirror(Element elem, Class annotation){
        for(AnnotationMirror mirror: elem.getAnnotationMirrors()){
            if(matches(mirror, annotation))
                return mirror;
        }
        return null;
    }

    public static AnnotationValue getRawAnnotationValue(Element pos, AnnotationMirror mirror, String method){
        for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()){
            if(entry.getKey().getSimpleName().contentEquals(method))
                return entry.getValue();
        }
        for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : Environment.get().getElementUtils().getElementValuesWithDefaults(mirror).entrySet()){
            if(entry.getKey().getSimpleName().contentEquals(method))
                return entry.getValue();
        }
        throw new AnnotationError(pos, mirror, "annotation "+((TypeElement)mirror.getAnnotationType().asElement()).getQualifiedName()+" is missing "+method);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T getAnnotationValue(Element pos, AnnotationMirror mirror, String method){
        return (T)getRawAnnotationValue(pos, mirror, method).getValue();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAnnotationValue(Element elem, Class annotation, String method){
        AnnotationMirror mirror = getAnnotationMirror(elem, annotation);
        if(mirror!=null)
            return (T)getAnnotationValue(elem, mirror, method);
        else
            return null;
    }


    /*-------------------------------------------------[ javadoc ]---------------------------------------------------*/

    private static final String PARAM = "@param";
    private static final String RETURN = "@return";

    public static String getMethodDoc(String doc){
        if(doc==null)
            return null;
        else{
            int index = doc.indexOf(PARAM);
            if(index==-1){
                doc = doc.trim();
            }else
                doc = doc.substring(0, index).trim();

            index = doc.indexOf(RETURN);
            if(index==-1){
                doc = doc.trim();
            }else
                doc = doc.substring(0, index).trim();
            return doc;
        }
    }

    private static String[] split(String str, boolean whitespace){
        int i = 0;
        while(i<str.length()){
            char ch = str.charAt(i);
            if(Character.isWhitespace(ch)==whitespace)
                i++;
            else
                break;
        }
        return new String[]{ str.substring(0, i), str.substring(i) };
    }

    public static Map<String, String> getMethodParamDocs(String doc){
        Map<String, String> docs = new HashMap<String, String>();
        if(doc!=null){
            int index = doc.indexOf(RETURN);
            if(index!=-1)
                doc = doc.substring(0, index);
            index = doc.indexOf(PARAM);
            if(index!=-1)
                doc = doc.substring(index).trim();
            for(String token: doc.split(PARAM)){
                token = token.trim();
                if(token.length()>0){
                    String str[] = split(token, false);
                    str[1] = str[1].trim();
                    if(str[0].length()>0 && str[1].length()>0)
                        docs.put(str[0], str[1]);
                }
            }
        }
        return docs;
    }

    /*-------------------------------------------------[ Finding Generated Class ]---------------------------------------------------*/

    public static String[] findClass(TypeElement clazz, String format){
        String qname = format.replace("${package}", ModelUtil.getPackage(clazz))
                .replace("${class}", clazz.getSimpleName().toString());
        String pakage, clazzName;

        int dot = qname.lastIndexOf('.');
        if(dot==-1){
            pakage = "";
            clazzName = qname;
        }else{
            pakage = qname.substring(0, dot);
            clazzName = qname.substring(dot+1);
            if(pakage.length()==0)
                qname = clazzName;
        }
        return new String[]{ qname, pakage, clazzName };
    }

    public static boolean exists(String pakage, String relativeName){
        try{
            InputStream is = null;
            try{
                is = Environment.get().getFiler().getResource(StandardLocation.SOURCE_PATH, pakage, relativeName).openInputStream();
                System.out.println("is: "+is);
                return true;
            }finally{
                if(is!=null)
                    is.close();
            }
        }catch(Exception ignore){
            return false; //http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6647998
        }
    }
}
