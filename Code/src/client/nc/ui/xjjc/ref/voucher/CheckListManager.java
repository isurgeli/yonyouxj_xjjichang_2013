package nc.ui.xjjc.ref.voucher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

//@author Santhosh Kumar T - santhosh@in.fiorano.com
public class CheckListManager extends MouseAdapter implements ListSelectionListener, ActionListener{ 
 private ListSelectionModel selectionModel = new DefaultListSelectionModel(); 
 private JList list = new JList(); 
 int hotspot = new JCheckBox().getPreferredSize().width; 

 public CheckListManager(JList list){ 
     this.list = list; 
     list.setCellRenderer(new CheckListCellRenderer(list.getCellRenderer(), selectionModel)); 
     list.registerKeyboardAction(this, KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_FOCUSED); 
     list.addMouseListener(this); 
     selectionModel.addListSelectionListener(this); 
 } 

 public ListSelectionModel getSelectionModel(){ 
     return selectionModel; 
 } 

 private void toggleSelection(int index){ 
     if(index<0) 
         return; 

     if(selectionModel.isSelectedIndex(index)) 
         selectionModel.removeSelectionInterval(index, index); 
     else 
         selectionModel.addSelectionInterval(index, index); 
 } 

 /*------------------------------[ MouseListener ]-------------------------------------*/ 

 public void mouseClicked(MouseEvent me){ 
     int index = list.locationToIndex(me.getPoint()); 
     if(index<0) 
         return; 
     if(me.getX()>list.getCellBounds(index, index).x+hotspot) 
         return; 
     toggleSelection(index); 
 } 

 /*-----------------------------[ ListSelectionListener ]---------------------------------*/ 

 public void valueChanged(ListSelectionEvent e){ 
     list.repaint(list.getCellBounds(e.getFirstIndex(), e.getLastIndex())); 
 } 

 /*-----------------------------[ ActionListener ]------------------------------*/ 

 public void actionPerformed(ActionEvent e){ 
     toggleSelection(list.getSelectedIndex()); 
 } 
} 
