package edu.washington.cs.downconvert.rewrite;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class GlobalRewrite {

	public interface Editor{
		TextEdit rewrite(CompilationUnit cu, Document original);
	}
	
	private static CompilationUnit parse(ICompilationUnit unit) {
		ASTParser parser = ASTParser.newParser(AST.JLS3); 
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(unit); // set source
		parser.setResolveBindings(true); // we need bindings later on
		return (CompilationUnit) parser.createAST(null /* IProgressMonitor */); // parse
	}

	public static void rewriteWorkspace(Editor editor){
		try{
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceRoot root = workspace.getRoot();
			
			// Get all projects in the workspace
			IProject[] projects = root.getProjects();
			// Loop over all projects
			for (IProject project : projects) {

				// Only work on open projects with the Java nature
				if (project.isOpen()
						&& project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
					IJavaProject javaProject = JavaCore.create(project);
					
					for (IPackageFragment p : javaProject.getPackageFragments())
					{
						if (p.getKind() == IPackageFragmentRoot.K_SOURCE) {
							for (ICompilationUnit cu : p.getCompilationUnits()){
								ApplyEdit(editor, cu);
							}
						}
					}
				}
			}
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}
	}
	
	@SuppressWarnings("deprecation")
	private static void ApplyEdit(Editor editor, ICompilationUnit cu) throws CoreException, MalformedTreeException, BadLocationException{
		TextEdit edit = editor.rewrite(parse(cu), new Document(cu.getSource()));
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager(); // get the buffer manager
		IPath path = cu.getPath(); // unit: instance of CompilationUnit
		try {
			bufferManager.connect(path, null); 
			ITextFileBuffer textFileBuffer = bufferManager.getTextFileBuffer(path);
			// retrieve the buffer
			IDocument document = textFileBuffer.getDocument(); 
			// ... edit the document here ... 
		
			edit.apply(document);
			
		    // commit changes to underlying file
			textFileBuffer.commit(null /* ProgressMonitor */, false /* Overwrite */); // (3)

		} finally {
			bufferManager.disconnect(path, null); // (4)
		}
	}
	
}
