package jlibs.graph.sequences;

import jlibs.graph.Path;
import jlibs.graph.Sequence;

/**
 * @author Santhosh Kumar T
 */
public class PathSequence<E> extends AbstractSequence<Path>{
    private Path path;
    private Sequence<E> delegate;

    public PathSequence(Path path, Sequence<E> delegate){
        this.path = path;
        this.delegate = delegate;
        _reset();
    }

    private int pos;

    @Override
    public void reset(){
        super.reset();
        _reset();
    }

    private void _reset(){
        delegate.reset();
        pos = 0;
    }

    @Override
    protected Path findNext(){
        E elem = delegate.next();
        return elem==null ? null : path.append(elem, pos++);
    }

    @Override
    public Sequence<Path> copy(){
        return new PathSequence<E>(path, delegate.copy());
    }
}
