/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.soytree;

import com.google.common.collect.Lists;
import com.google.template.soy.base.SoySyntaxException;
import com.google.template.soy.exprparse.ExpressionParser;
import com.google.template.soy.exprparse.ParseException;
import com.google.template.soy.exprparse.TokenMgrError;
import com.google.template.soy.exprtree.ExprNode;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.soytree.SoyNode.ConditionalBlockNode;
import com.google.template.soy.soytree.SoyNode.LocalVarBlockNode;
import com.google.template.soy.soytree.SoyNode.LoopNode;
import com.google.template.soy.soytree.SoyNode.ParentExprHolderNode;
import com.google.template.soy.soytree.SoyNode.SoyStatementNode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Node representing a 'for' statement.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 * @author Kai Huang
 */
public class ForNode extends AbstractParentSoyCommandNode<SoyNode>
    implements SoyStatementNode, ConditionalBlockNode<SoyNode>, LoopNode<SoyNode>,
    ParentExprHolderNode<SoyNode>, LocalVarBlockNode<SoyNode> {


  /** Regex pattern for the command text. */
  // 2 capturing groups: local var name, arguments to range()
  private static final Pattern COMMAND_TEXT_PATTERN =
      Pattern.compile("( [$] \\w+ ) \\s+ in \\s+ range[(] \\s* (.*) \\s* [)]",
                      Pattern.COMMENTS | Pattern.DOTALL);


  /** The local (loop) variable name. */
  private final String localVarName;

  /** The texts of the individual range args (sort of canonicalized). */
  private final List<String> rangeArgTexts;

  /** The parsed range args. */
  private final List<ExprRootNode<ExprNode>> rangeArgs;


  /**
   * @param id The id for this node.
   * @param commandText The command text.
   * @throws SoySyntaxException If a syntax error is found.
   */
  public ForNode(String id, String commandText) throws SoySyntaxException {
    super(id, "for", commandText);

    Matcher matcher = COMMAND_TEXT_PATTERN.matcher(commandText);
    if (!matcher.matches()) {
      throw new SoySyntaxException("Invalid 'for' command text \"" + commandText + "\".");
    }

    try {
      localVarName = (new ExpressionParser(matcher.group(1))).parseVariable().getChild(0).getName();
    } catch (TokenMgrError tme) {
      throw createExceptionForInvalidCommandText("variable name", tme);
    } catch (ParseException pe) {
      throw createExceptionForInvalidCommandText("variable name", pe);
    }

    try {
      rangeArgs = (new ExpressionParser(matcher.group(2))).parseExpressionList();
    } catch (TokenMgrError tme) {
      throw createExceptionForInvalidCommandText("range specification", tme);
    } catch (ParseException pe) {
      throw createExceptionForInvalidCommandText("range specification", pe);
    }
    if (rangeArgs.size() > 3) {
      throw new SoySyntaxException(
          "Invalid range specification in 'for' command text \"" + commandText + "\".");
    }

    rangeArgTexts = Lists.newArrayList();
    for (ExprNode rangeArg : rangeArgs) {
      rangeArgTexts.add(rangeArg.toSourceString());
    }
  }


  /**
   * Private helper for the constructor.
   * @param desc Description of the invalid item.
   * @param cause The underlying exception.
   * @return The SoySyntaxException to be thrown.
   */
  private SoySyntaxException createExceptionForInvalidCommandText(
      String desc, Throwable cause) {
    //noinspection ThrowableInstanceNeverThrown
    return new SoySyntaxException(
        "Invalid " + desc + " in 'for' command text \"" + getCommandText() + "\".", cause);
  }


  @Override public String getLocalVarName() {
    return localVarName;
  }

  /** Returns the texts of the individual range args (sort of canonicalized). */
  public List<String> getRangeArgTexts() {
    return rangeArgTexts;
  }

  /** Returns the parsed range args. */
  public List<ExprRootNode<ExprNode>> getRangeArgs() {
    return rangeArgs;
  }


  @Override public List<? extends ExprRootNode<? extends ExprNode>> getAllExprs() {
    return rangeArgs;
  }

}
