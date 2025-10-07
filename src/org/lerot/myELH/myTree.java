  package org.lerot.myELH;
  
  import java.util.Enumeration;
  import java.util.StringTokenizer;
  import javax.swing.JTree;
  import javax.swing.tree.DefaultMutableTreeNode;
  import javax.swing.tree.TreePath;

  
  class myTree    extends JTree
  {
    private static final long serialVersionUID = 1L;
    DefaultMutableTreeNode topnode;
    
    myTree(DefaultMutableTreeNode dmtn, boolean b) {
      super(dmtn, b);
      this.topnode = dmtn;
    }

    public void expandPathStr(String path) {
      String[] arr = path.split("/");
      expandPath(new TreePath(arr));
    }

    public DefaultMutableTreeNode findActiveTreenode(String path) {
      DefaultMutableTreeNode currentsearchnode = null;
      String treename = (String)this.topnode.getUserObject().toString();
      String str = path;
      StringTokenizer st = new StringTokenizer(str, "/");
      if (st.hasMoreElements()) {
        
        String topname = (String)st.nextElement();
        if (!topname.equalsIgnoreCase(treename)) {
          System.out.println(" Error in ELH tree name " + topname + "!=" + treename);
          return null;
        } 
        if (!st.hasMoreElements()) {
          return this.topnode;
        }
        currentsearchnode = this.topnode;
        while (st.hasMoreElements()) {
          
          String centityname = (String)st.nextElement();
          Enumeration<DefaultMutableTreeNode> childlist = (Enumeration)currentsearchnode.children();
          DefaultMutableTreeNode foundchild = null;
          while (childlist.hasMoreElements()) {
            
            DefaultMutableTreeNode nextchild = childlist.nextElement();
            String nextname = "";
            if (nextchild.getUserObject() instanceof elhnode) {
              
              nextname = ((elhnode)nextchild.getUserObject()).getName();
            }
            else {
              
              nextname = (String)nextchild.getUserObject();
            } 
            if (centityname.contentEquals(nextname)) {
              
              foundchild = nextchild;
              
              break;
            } 
          } 
          if (foundchild == null) {
            
            System.out.println(" Error in ELH search " + centityname + " not found ");
            return currentsearchnode;
          } 
          currentsearchnode = foundchild;
        } 
      } 
      
      return currentsearchnode;
    }
  }

