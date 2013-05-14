package net.vtst.ow.eclipse.less.parser;

import org.eclipse.xtext.naming.IQualifiedNameConverter;

import com.google.inject.Singleton;

@Singleton
public class LessQualifiedNameConverter extends IQualifiedNameConverter.DefaultImpl implements IQualifiedNameConverter {

  @Override
  public String getDelimiter() {
    return "";
  }

}
