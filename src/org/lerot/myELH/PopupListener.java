  package org.lerot.myELH;
  
  import java.awt.event.MouseAdapter;
  import java.awt.event.MouseEvent;
  import javax.swing.JPopupMenu;

  class PopupListener  extends MouseAdapter
  {
    private final JPopupMenu popup;
    
    PopupListener(JPopupMenu popnode) {
      this.popup = popnode;
    }
    public void mousePressed(MouseEvent e) {
      maybeShowPopup(e);
    }
    
    public void mouseReleased(MouseEvent e) {
      maybeShowPopup(e);
    }
    
    private void maybeShowPopup(MouseEvent e)
    {
      if (e.isPopupTrigger())
        this.popup.show(e.getComponent(), 
            e.getX(), e.getY()); 
    }
  }

