package classexpression;

import io.vertx.core.AbstractVerticle;
import io.vertx.examples.ClassExpressionTest;
import io.vertx.examples.CodeTrans;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class InvokeStaticMethodVerticle extends AbstractVerticle {

  @Override
  @CodeTrans
  public void start() throws Exception {
    ClassExpressionTest.noArg();
    ClassExpressionTest.arg("foo");
  }
}