  package org.lerot.myELH;
  
  import java.awt.Dimension;
  import java.awt.Rectangle;
  import java.util.Vector;
  
  
  
  
  
  public class elhdrawnode
  {
    int row;
    double col;
    String text;
    String elhtype;
    private String type;
    double width;
    double height;
    double x = 0.0D;
    double y = 0.0D;
    Vector<elhdrawnode> children;
    private static int columncounter;
    double mincol;
    double maxcol;
    int maxrow;
    private elhdrawnode parent;
    private String stylename;
    
    public elhdrawnode(elhnode anode, int r, int maxrow) {
      this.parent = null;
      this.children = new Vector<>();
      this.row = r;
      this.col = 1.0D;
      this.mincol = 1000.0D;
      this.maxcol = 0.0D;
      this.text = anode.getName();
      this.type = anode.getType();
      this.elhtype = anode.getClass().getSimpleName();
      this.stylename = anode.getStylename();
      int count = anode.countChildren();
      if (r <= maxrow && count > 0) {
        for (elhnode acnode : anode.getChildren()) {
          if (acnode instanceof elhnode) {
            elhdrawnode newdnode = new elhdrawnode(acnode, r + 1, maxrow);
            newdnode.setParent(this);
            this.children.add(newdnode);
          } 
        } 
      }
    }
    
    private void setParent(elhdrawnode adnode) {
      this.parent = adnode;
    }
    
    public void updatetree() {
      this.mincol = 1000.0D;
      this.maxcol = 0.0D;
      this.col = 1.0D;
      this.row = 0;
      columncounter = 1;
      for (elhdrawnode adnode : this.children) {
        adnode.updateNode(this.row + 1);
        if (adnode.col < this.mincol) {
          this.mincol = adnode.col;
        }
        if (adnode.col > this.maxcol) {
          this.maxcol = adnode.col;
        }
      } 
      this.col = (this.mincol + this.maxcol) / 2.0D;
      if (this.children.size() < 2) {
        this.mincol = this.col;
        this.maxcol = this.col;
      } 
    }
    
    private void updateNode(int nrow) {
      this.mincol = 1000.0D;
      this.maxcol = 0.0D;
      this.row = nrow;
      if (this.children.isEmpty()) {
        this.col = columncounter;
        this.mincol = columncounter;
        this.maxcol = columncounter;
        columncounter += 2;
      } else {
        
        for (elhdrawnode adnode : this.children) {
          adnode.updateNode(this.row + 1);
          if (adnode.col < this.mincol) {
            this.mincol = adnode.col;
          }
          if (adnode.col > this.maxcol) {
            this.maxcol = adnode.col;
          }
        } 
        this.col = (this.mincol + this.maxcol) / 2.0D;
        if (this.children.size() < 2) {
          this.mincol = this.col;
          this.maxcol = this.col;
        } 
      } 
    }
    
    public boolean hasChildren() {
      return !this.children.isEmpty();
    }
    
    public Vector<elhdrawnode> getChildren() {
      return this.children;
    }
    
    public double getminchildcol() {
      double acol = this.col;
      for (elhdrawnode adnode : this.children) {
        double cmincol = adnode.getminchildcol();
        if (cmincol < acol) {
          acol = cmincol;
        }
      } 
      return acol;
    }
    
    public double getmaxchildcol() {
      double acol = this.maxcol;
      for (elhdrawnode adnode : this.children) {
        double cmaxcol = adnode.getmaxchildcol();
        if (cmaxcol > acol) {
          acol = cmaxcol;
        }
      } 
      return acol;
    }
  
    
    public int getmaxchildRow() {
      int mrow = this.row;
      if (hasChildren()) {
        for (elhdrawnode adnode : this.children) {
          int cmaxrow = adnode.getmaxchildRow();
          if (cmaxrow > mrow) {
            mrow = cmaxrow;
          }
        } 
      }
      return mrow;
    }
    
    String getType() {
      if (this.type == null) {
        return "Sequence";
      }
      return this.type;
    }
    
    void setType(String type) {
      this.type = type;
    }
    
    public elhdrawnode getparent() {
      return this.parent;
    }
    
    public String getText() {
      return this.text;
    }
    
    public int countChildren() {
      return this.children.size();
    }
    
    public String getStylename() {
      if (this.stylename != null && !this.stylename.isEmpty()) {
        return this.stylename;
      }
      return this.elhtype;
    }
    
    public void setStylename(String stylename) {
      this.stylename = stylename;
    }
    
    public boolean hasParent() {
      return (this.parent != null);
    }
  
    
    public String toString() {
      return String.valueOf(this.text) + " row =" + this.row + " col=" + this.col;
    }
  
    
    public void setDimension(double x2, double y2, double w, double h) {
      this.x = x2;
      this.y = y2;
      this.width = w;
      this.height = h;
    }
  
    
    boolean containsPoint(double px, double py) {
        return px < this.x + this.width && px > this.x && py < this.y + this.height && py > this.y;
    }
  
    
    elhdrawnode getTarget(double px, double py) {
      if (hasChildren())
      {
        for (elhdrawnode adnode : this.children) {
          
          elhdrawnode fnode = adnode.getTarget(px, py);
          if (fnode != null) return fnode; 
        } 
      }
      if (containsPoint(px, py)) return this; 
      return null;
    }
  
    
    public Dimension getDimension() {
      return new Dimension((int)this.width, (int)this.height);
    }
  
    
    public Rectangle getBounds() {
      return new Rectangle((int)this.x, (int)this.y, (int)this.width, (int)this.height);
    }
  }


/* Location:              /home/paul/applications/myELH.jar!/org/lerot/myELH/elhdrawnode.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */