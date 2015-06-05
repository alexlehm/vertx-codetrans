package io.vertx.codetrans.lang.groovy;

import com.sun.source.tree.LambdaExpressionTree;
import io.vertx.codegen.TypeInfo;
import io.vertx.codetrans.BinaryExpressionModel;
import io.vertx.codetrans.BlockModel;
import io.vertx.codetrans.CodeModel;
import io.vertx.codetrans.CodeWriter;
import io.vertx.codetrans.DataObjectLiteralModel;
import io.vertx.codetrans.ExpressionModel;
import io.vertx.codetrans.Helper;
import io.vertx.codetrans.JsonArrayLiteralModel;
import io.vertx.codetrans.JsonObjectLiteralModel;
import io.vertx.codetrans.Lang;
import io.vertx.codetrans.Member;
import io.vertx.codetrans.StatementModel;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
class GroovyWriter extends CodeWriter {

  LinkedHashSet<TypeInfo.Class> imports = new LinkedHashSet<>();

  GroovyWriter(Lang lang) {
    super(lang);
  }

  @Override
  public void renderBinary(BinaryExpressionModel expression) {
    if (Helper.isString(expression)) {
      Helper.renderInterpolatedString(expression, this, "${", "}");
    } else {
      super.renderBinary(expression);
    }
  }

  @Override
  public void renderStatement(StatementModel statement) {
    statement.render(this);
    append("\n");
  }

  // Temporary hack
  private int depth = 0;

  @Override
  public void renderBlock(BlockModel block) {
    if (depth++ > 0) {
      super.renderBlock(block);
    } else {
      super.renderBlock(block);
      StringBuilder buffer = getBuffer();
      String tmp = buffer.toString();
      buffer.setLength(0);
      for (TypeInfo.Class importedType : imports) {
        String fqn = importedType.getName();
        if (importedType instanceof TypeInfo.Class.Api) {
          fqn = importedType.translateName("groovy");
        }
        append("import ").append(fqn).append('\n');
      }
      append(tmp);
    }
    depth--;
  }

  @Override
  public void renderLongLiteral(String value) {
    renderChars(value);
    append('L');
  }

  @Override
  public void renderFloatLiteral(String value) {
    renderChars(value);
    append('f');
  }

  @Override
  public void renderDoubleLiteral(String value) {
    renderChars(value);
    append('d');
  }

  @Override
  public void renderThis() {
    append("this");
  }

  @Override
  public void renderLambda(LambdaExpressionTree.BodyKind bodyKind, List<TypeInfo> parameterTypes, List<String> parameterNames, CodeModel body) {
    append("{");
    for (int i = 0; i < parameterNames.size(); i++) {
      if (i == 0) {
        append(" ");
      } else {
        append(", ");
      }
      append(parameterNames.get(i));
    }
    append(" ->\n");
    indent();
    body.render(this);
    if (bodyKind == LambdaExpressionTree.BodyKind.EXPRESSION) {
      append("\n");
    }
    unindent();
    append("}");
  }

  @Override
  public void renderApiType(TypeInfo.Class.Api apiType) {
    imports.add(apiType);
    append(apiType.getSimpleName());
  }

  @Override
  public void renderEnumConstant(TypeInfo.Class.Enum type, String constant) {
    imports.add(type);
    append(type.getSimpleName()).append('.').append(constant);
  }

  @Override
  public void renderThrow(String throwableType, ExpressionModel reason) {
    if (reason == null) {
      append("throw new ").append(throwableType).append("()");
    } else {
      append("throw new ").append(throwableType).append("(");
      reason.render(this);
      append(")");
    }
  }

  public void renderDataObject(DataObjectLiteralModel model) {
    renderJsonObject(model.getMembers(), false);
  }

  public void renderJsonObject(JsonObjectLiteralModel jsonObject) {
    renderJsonObject(jsonObject.getMembers(), true);
  }

  public void renderJsonArray(JsonArrayLiteralModel jsonArray) {
    renderJsonArray(jsonArray.getValues());
  }

  private void renderJsonObject(Iterable<Member> members, boolean unquote) {
    Iterator<Member> iterator = members.iterator();
    if (iterator.hasNext()) {
      append("[\n").indent();
      while (iterator.hasNext()) {
        Member member = iterator.next();
        String name = member.getName().render(getLang());
        if (unquote) {
          name = Helper.unwrapQuotedString(name);
        }
        append(name);
        append(":");
        if (member instanceof Member.Single) {
          ((Member.Single) member).getValue().render(this);
        } else {
          renderJsonArray(((Member.Array) member).getValues());
        }
        if (iterator.hasNext()) {
          append(',');
        }
        append('\n');
      }
      unindent().append("]");
    } else {
      append("[:]");
    }
  }

  private void renderJsonArray(List<ExpressionModel> values) {
    append("[\n").indent();
    for (int i = 0;i < values.size();i++) {
      values.get(i).render(this);
      if (i < values.size() - 1) {
        append(',');
      }
      append('\n');
    }
    unindent().append(']');
  }

  @Override
  public void renderJsonObjectAssign(ExpressionModel expression, ExpressionModel name, ExpressionModel value) {
    expression.render(this);
    append('.');
    name.render(this);
    append(" = ");
    value.render(this);
  }

  @Override
  public void renderDataObjectAssign(ExpressionModel expression, ExpressionModel name, ExpressionModel value) {
    renderJsonObjectAssign(expression, name, value);
  }

  @Override
  public void renderJsonObjectMemberSelect(ExpressionModel expression, ExpressionModel name) {
    expression.render(this);
    append('.');
    name.render(this);
  }

  @Override
  public void renderJsonObjectToString(ExpressionModel expression) {
    expression.render(this);
    append(".toString()");
  }

  @Override
  public void renderJsonArrayToString(ExpressionModel expression) {
    expression.render(this);
    append(".toString()");
  }

  @Override
  public void renderDataObjectMemberSelect(ExpressionModel expression, ExpressionModel name) {
    renderJsonObjectMemberSelect(expression, name);
  }

  @Override
  public void renderMapGet(ExpressionModel map, ExpressionModel arg) {
    map.render(this);
    append('[');
    arg.render(this);
    append(']');
  }

  @Override
  public void renderMapForEach(ExpressionModel map, String keyName, TypeInfo keyType, String valueName, TypeInfo valueType, LambdaExpressionTree.BodyKind bodyKind, CodeModel block) {
    map.render(this);
    append(".each ");
    renderLambda(bodyKind, Arrays.asList(keyType, valueType), Arrays.asList(keyName, valueName), block);
  }

  @Override
  public void renderMethodReference(ExpressionModel expression, String methodName) {
    expression.render(this);
    append(".&").append(methodName);
  }

  @Override
  public void renderNew(ExpressionModel expression, TypeInfo type, List<ExpressionModel> argumentModels) {
    append("new ");
    expression.render(this);
    append('(');
    for (int i = 0; i < argumentModels.size(); i++) {
      if (i > 0) {
        append(", ");
      }
      argumentModels.get(i).render(this);
    }
    append(')');
  }
}
