package net.vtst.ow.closure.compiler.magic;

import java.util.Map;

import com.google.javascript.jscomp.DiagnosticGroup;
import com.google.javascript.jscomp.DiagnosticGroups;

public class MagicDiagnosticGroups extends DiagnosticGroups {
  
  public Map<String, DiagnosticGroup> getRegisteredGroups() {
    return super.getRegisteredGroups();
  }

}
