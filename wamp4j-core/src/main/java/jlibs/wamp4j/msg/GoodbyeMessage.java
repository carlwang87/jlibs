/**
 * Copyright 2015 Santhosh Kumar Tekuri
 *
 * The JLibs authors license this file to you under the Apache License,
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

package jlibs.wamp4j.msg;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jlibs.wamp4j.error.InvalidMessageException;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;
import static jlibs.wamp4j.Util.nonNull;

/**
 * WAMP session is closed explicitly by a GOODBYE message sent by one Peer
 * and a GOODBYE message sent from the other Peer in response
 *
 * NOTE: GOODBYE is sent only after a Session is already established
 * NOTE: GOODBYE must be replied by the receiving Peer
 *
 * @author Santhosh Kumar Tekuri
 */
public class GoodbyeMessage extends WAMPMessage{
    public static final int ID = 6;

    /**
     * dictionary that allows to provide additional, optional closing information
     */
    public final ObjectNode details;

    /**
     * MUST be an URI
     */
    public final String reason;

    public GoodbyeMessage(ObjectNode details, String reason){
        this.details = details;
        this.reason = nonNull(reason, "null reason");
    }

    public GoodbyeMessage(String message, String reason){
        details = instance.objectNode();
        details.put("message", message);
        this.reason = nonNull(reason, "null reason");
    }

    @Override
    public int getID(){
        return ID;
    }

    @Override
    public void toArrayNode(ArrayNode array){
        array.add(idNodes[ID]);
        array.add(objectNode(details));
        array.add(reason);
    }

    static final Decoder decoder = new Decoder(){
        @Override
        public WAMPMessage decode(ArrayNode array) throws InvalidMessageException{
            if(array.size()!=3)
                throw new InvalidMessageException();

            assert id(array)==ID;
            return new GoodbyeMessage(objectValue(array, 1), textValue(array, 2));
        }
    };
}
