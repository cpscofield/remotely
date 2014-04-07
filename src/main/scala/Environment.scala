package remotely

import scala.reflect.runtime.universe.TypeTag
import scodec.{Codec,Decoder,Encoder}

// could do dynamic lookup of encoder, decoder, using type tags

case class Environment(codecs: Codecs, values: Values) {

  def decoders = codecs.decoders
  def encoders = codecs.encoders

  def encoder[A:TypeTag:Encoder]: Environment =
    this.copy(codecs = codecs.encoder[A])

  def decoder[A:TypeTag:Decoder]: Environment =
    this.copy(codecs = codecs.decoder[A])

  def codec[A](implicit T: TypeTag[A], C: Codec[A]): Environment =
    this.copy(codecs = codecs.codec[A])

  /** Add the given codecs to this `Environment`, keeping existing codecs. */
  def codecs(c: Codecs): Environment =
    Environment(codecs ++ c, values)

  /** Add the given (strict) value or function to this `Environment`, keeping existing codecs. */
  def declare[A:TypeTag](name: String)(a: A): Environment =
    this.copy(values = values.declareStrict[A](name)(a))

  /**
   * Modify the values inside this `Environment`, using the given function `f`.
   * Example: `Environment.empty.modifyValues { _.declare0("x")(Task.now(42)) }`.
   */
  def modifyValues(f: Values => Values): Environment =
    this.copy(values = f(values))

  /** Alias for `this.modifyValues(_ => v)`. */
  def values(v: Values): Environment =
    this.modifyValues(_ => v)

  /**
   * Serve this `Environment` via a TCP server at the given address.
   * Returns a thunk that can be used to stop the server.
   */
  def serve(addr: java.net.InetSocketAddress)(monitoring: Monitoring): () => Unit =
    Server.start(this)(addr)(monitoring)

  /** Generate the Scala code for the client access to this `Environment`. */
  def generateClient(moduleName: String): String =
    Signatures(values.keySet).generateClient(moduleName)

  override def toString = {
    s"""Environment {
    |  ${values.keySet.toList.sorted.mkString("\n  ")}
    |
    |  decoders:
    |    ${decoders.keySet.toList.sorted.mkString("\n    ")}
    |
    |  encoders:
    |    ${encoders.keySet.toList.sorted.mkString("\n    ")}
    |}
    """.stripMargin
  }
}

object Environment {
  val empty = Environment(Codecs.empty, Values.empty)
}
