package edu.washington.cs.downconvert.actions;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import edu.washington.cs.downconvert.rewrite.ConvertEnhancedFor;
import edu.washington.cs.downconvert.rewrite.GlobalRewrite;
import edu.washington.cs.downconvert.rewrite.InsertCastsForGenerics;
import edu.washington.cs.downconvert.rewrite.InsertGenericsComments;
import edu.washington.cs.downconvert.rewrite.InsertGenericsJml;
import edu.washington.cs.downconvert.rewrite.RemoveGenerics;
import edu.washington.cs.downconvert.rewrite.RemoveGenericsFromClass;
import edu.washington.cs.downconvert.rewrite.GlobalRewrite.Editor;

public class ConvertAllAction implements IWorkbenchWindowActionDelegate{
	private IWorkbenchWindow window;
	/**
	 * The constructor.
	 */
	public ConvertAllAction() {
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		try{
			// Insert Casts
			GlobalRewrite.rewriteWorkspace(new Editor(){
				@Override
				public TextEdit rewrite(CompilationUnit cu, Document original) {
					cu.recordModifications();
					InsertCastsForGenerics.convert(cu);
					return cu.rewrite(original, null);
				}
			});
			
			//Rewrite Loops
			GlobalRewrite.rewriteWorkspace(new Editor(){
				@Override
				public TextEdit rewrite(CompilationUnit cu, Document original) {
					cu.recordModifications();
					ConvertEnhancedFor.convert(cu);
					return cu.rewrite(original, null);
				}
			});
			
			//Insert Comments
			GlobalRewrite.rewriteWorkspace(new Editor(){
				@Override
				public TextEdit rewrite(CompilationUnit cu, Document original) {
					return InsertGenericsComments.commentGenerics(original, cu);
				}
			});
			
			//Insert JML Generics Comments
			GlobalRewrite.rewriteWorkspace(new Editor(){
				@Override
				public TextEdit rewrite(CompilationUnit cu, Document original) {
					return InsertGenericsJml.commentGenerics(original, cu);
				}
			});
			
			//Remove Uses of Generics
			GlobalRewrite.rewriteWorkspace(new Editor(){
				@Override
				public TextEdit rewrite(CompilationUnit cu, Document original) {
					cu.recordModifications();
					RemoveGenerics.convert(cu);
					return cu.rewrite(original, null);
				}
			});
			
			//Remove Generics from Generic Classes
			GlobalRewrite.rewriteWorkspace(new Editor(){
				@Override
				public TextEdit rewrite(CompilationUnit cu, Document original) {
					cu.recordModifications();
					RemoveGenericsFromClass.convert(cu);
					return cu.rewrite(original, null);
				}
			});
			
		}catch(Exception ex){
			MessageDialog.openError(
					window.getShell(),
					"Java Downconverter",
					"Error downconverting: " + ex.getMessage());
		}
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}
