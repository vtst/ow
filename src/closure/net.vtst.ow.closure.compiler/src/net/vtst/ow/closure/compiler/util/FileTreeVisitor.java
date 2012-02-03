package net.vtst.ow.closure.compiler.util;


/**
 * Visitor for a library tree.
 * @author Vincent Simonet
 */
public class FileTreeVisitor {
  
  public interface IRelativePathProvider<T> {
    public T add(T path, String name);
  }
  
  public abstract static class Abstract<RelativePath, VisitingException extends Throwable> {
    
    protected abstract IRelativePathProvider<RelativePath> getRelativePathProvider();
    
    /**
     * This method is called during the visit, before visiting the contents of a directory.
     * The default implementation does nothing, and returns true.
     * It may be overridden by sub-classes.
     * @param path  The relative path of the visited directory, relatively to the root.
     * @param dir  The visited directory.
     * @return  A boolean indicating whether the contents of the directory shall be visited.
     */
    public boolean preVisitDirectory(java.io.File dir, RelativePath path) throws VisitingException {
      return true;
    }
  
    /**
     * This method is called during the visit, after visiting the contents of a directory.
     * The default implementation does nothing.  It may be overridden by sub-classes.
     * @param path  The relative path of the visited directory, relatively to the root.
     * @param dir  The visited directory.
     */
    public void postVisitDirectory(java.io.File dir, RelativePath path) throws VisitingException {}
    
    /**
     * This method is called during the visit, when visiting a file.
     * The default implementation does nothing.  It may be overridden by sub-classes.
     * @param path  The relative path of the visited directory, relatively to the root.
     * @param file  The visited file.
     */
    public void visitFile(java.io.File file, RelativePath path) throws VisitingException {}
  
    /**
     * Run the visitor starting from a root directory.
     * @param path  The relative path of the visited directory, relatively to the root.
     * @param rootDirectory  The root of the tree to visit.
     */
    public final void visit(java.io.File file, RelativePath path) throws VisitingException {
      if (file.isFile()) {
        visitFile(file, path);
      } else {
        if (!preVisitDirectory(file, path)) return;
        for (java.io.File file1: file.listFiles()) {
          RelativePath path1 = getRelativePathProvider().add(path, file1.getName());
          visit(file1, path1);
        }
        postVisitDirectory(file, path);
      }
    }
  }
    
  /**
   * A LibraryVisitor where without relative path.
   */
  public static class Simple<VisitingException extends Throwable> 
  extends Abstract<Object, VisitingException> {

    @Override
    public boolean preVisitDirectory(java.io.File dir, Object path) throws VisitingException {
      return previsitDirectory(dir);
    }
    public boolean previsitDirectory(java.io.File dir) { return true; }

    @Override
    public void postVisitDirectory(java.io.File dir, Object path) throws VisitingException {
      postVisitDirectory(dir);
    }
    public void postVisitDirectory(java.io.File dir) {}

    @Override
    public void visitFile(java.io.File file, Object path) throws VisitingException {
      visitFile(file);
    }
    public void visitFile(java.io.File file) {}

    public final void visit(java.io.File file) throws VisitingException {
      visit(file, new Object());
    }

    @Override
    protected IRelativePathProvider<Object> getRelativePathProvider() {
      return new IRelativePathProvider<Object>() {
        @Override
        public Object add(Object path, String name) {
          return this;
        }
      };
    }
    
  }
  
  /**
   * A LibraryVisitor where the relative path is a file.
   */
  public static class File<VisitingException extends Throwable> 
  extends Abstract<java.io.File, VisitingException> {

    @Override
    protected IRelativePathProvider<java.io.File> getRelativePathProvider() {
      return new IRelativePathProvider<java.io.File>() {
        @Override
        public java.io.File add(java.io.File path, String name) {
          return new java.io.File(path, name);
        }
      };
    }
    
  }
  
}