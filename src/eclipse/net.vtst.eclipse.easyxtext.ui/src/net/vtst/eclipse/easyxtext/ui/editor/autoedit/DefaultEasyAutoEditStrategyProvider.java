package net.vtst.eclipse.easyxtext.ui.editor.autoedit;

import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.xtext.ui.editor.autoedit.CompoundMultiLineTerminalsEditStrategy;
import org.eclipse.xtext.ui.editor.autoedit.MultiLineTerminalsEditStrategy;
import org.eclipse.xtext.ui.editor.autoedit.PartitionDeletionEditStrategy;
import org.eclipse.xtext.ui.editor.autoedit.PartitionEndSkippingEditStrategy;
import org.eclipse.xtext.ui.editor.autoedit.PartitionInsertEditStrategy;
import org.eclipse.xtext.ui.editor.autoedit.SingleLineTerminalsStrategy;
import org.eclipse.xtext.ui.editor.autoedit.AbstractEditStrategyProvider.IEditStrategyAcceptor;
import org.eclipse.xtext.ui.editor.model.TerminalsTokenTypeToPartitionMapper;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DefaultEasyAutoEditStrategyProvider extends EasyEditStrategyProvider {
  @Inject
  protected Provider<DefaultIndentLineAutoEditStrategy> defaultIndentLineAutoEditStrategy;
  @Inject
  protected Provider<PartitionEndSkippingEditStrategy> partitionEndSkippingEditStrategy;
  @Inject
  protected PartitionInsertEditStrategy.Factory partitionInsert;
  @Inject
  protected PartitionDeletionEditStrategy.Factory partitionDeletion;
  @Inject
  protected SingleLineTerminalsStrategy.Factory singleLineTerminals;
  @Inject
  protected MultiLineTerminalsEditStrategy.Factory multiLineTerminals;
  
  @Inject
  protected CompoundMultiLineTerminalsEditStrategy.Factory compoundMultiLineTerminals;
  
  @ConfigureAutoEdit
  protected void compoundBracesBlocks(IEditStrategyAcceptor acceptor) {
    acceptor.accept(compoundMultiLineTerminals.newInstanceFor("{", "}").and("[", "]").and("(", ")"), IDocument.DEFAULT_CONTENT_TYPE);
  }

  @ConfigureAutoEdit
  protected void indentationEditStrategy(IEditStrategyAcceptor acceptor) {
    acceptor.accept(defaultIndentLineAutoEditStrategy.get(), IDocument.DEFAULT_CONTENT_TYPE);
    acceptor.accept(defaultIndentLineAutoEditStrategy.get(), TerminalsTokenTypeToPartitionMapper.COMMENT_PARTITION);
    acceptor.accept(defaultIndentLineAutoEditStrategy.get(), TerminalsTokenTypeToPartitionMapper.SL_COMMENT_PARTITION);
  }

  @ConfigureAutoEdit
  protected void multilineComments(IEditStrategyAcceptor acceptor) {
    acceptor.accept(singleLineTerminals.newInstance("/*", " */"),IDocument.DEFAULT_CONTENT_TYPE);
    acceptor.accept(multiLineTerminals.newInstance("/*"," * ", " */"),IDocument.DEFAULT_CONTENT_TYPE);
    acceptor.accept(multiLineTerminals.newInstance("/*"," * ", " */"),TerminalsTokenTypeToPartitionMapper.COMMENT_PARTITION);
  }

  @ConfigureAutoEdit
  protected void curlyBracesBlock(IEditStrategyAcceptor acceptor) {
    acceptor.accept(singleLineTerminals.newInstance("{", "}"),IDocument.DEFAULT_CONTENT_TYPE);
  }

  @ConfigureAutoEdit
  protected void squareBrackets(IEditStrategyAcceptor acceptor) {
    acceptor.accept(singleLineTerminals.newInstance("[", "]"),IDocument.DEFAULT_CONTENT_TYPE);
  }

  @ConfigureAutoEdit
  protected void parenthesis(IEditStrategyAcceptor acceptor) {
    acceptor.accept(singleLineTerminals.newInstance("(", ")"),IDocument.DEFAULT_CONTENT_TYPE);
  }
  
  @ConfigureAutoEdit
  protected void stringLiteral(IEditStrategyAcceptor acceptor) {
    acceptor.accept(partitionInsert.newInstance("\"","\""),IDocument.DEFAULT_CONTENT_TYPE);
    acceptor.accept(partitionInsert.newInstance("'","'"),IDocument.DEFAULT_CONTENT_TYPE);
    // The following two are registered for the default content type, because on deletion 
    // the command.offset is cursor-1, which is outside the partition of terminals.length = 1.
    // How crude is that?
    // Note that in case you have two string literals following each other directly, the deletion strategy wouldn't apply.
    // One could add the same strategy for the STRING partition in addition to solve this
    acceptor.accept(partitionDeletion.newInstance("\"","\""),IDocument.DEFAULT_CONTENT_TYPE);
    acceptor.accept(partitionDeletion.newInstance("'","'"),IDocument.DEFAULT_CONTENT_TYPE);
    acceptor.accept(partitionEndSkippingEditStrategy.get(),TerminalsTokenTypeToPartitionMapper.STRING_LITERAL_PARTITION);
  }

}
