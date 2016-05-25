package utils

import scala.reflect.runtime.universe._

/**
  * Scala Enumeration helpers implementing Scala versions of
  * Java's java.lang.Enum.valueOf(Class[Enum], String).
  *
  * Usage: {{{val level = EnumReflector.withName[FunninessLevel.Value]("LOL")}}}
  */
object EnumReflector {

  private val mirror: Mirror = runtimeMirror(getClass.getClassLoader)

  /**
    * Returns a value of the specified enumeration with the given name.
    * @param name value name
    * @tparam T enumeration type
    * @return enumeration value, see scala.Enumeration.withName(String)
    */
  def withName[T <: Enumeration#Value: TypeTag](name: String): T = {
    typeOf[T] match {
      case valueType @ TypeRef(enumType, _, _) =>
        val methodSymbol = factoryMethodSymbol(enumType)
        val moduleSymbol = enumType.termSymbol.asModule
        reflect(moduleSymbol, methodSymbol)(name).asInstanceOf[T]
    }
  }

  /**
    * Returns a value of the specified enumeration with the given name.
    * @param clazz enumeration class
    * @param name value name
    * @return enumeration value, see scala.Enumeration#withName(String)
    */
  def withName(clazz: Class[_], name: String): Enumeration#Value = {
    val classSymbol = mirror.classSymbol(clazz)
    val methodSymbol = factoryMethodSymbol(classSymbol.toType)
    val moduleSymbol = classSymbol.companionSymbol.asModule
    reflect(moduleSymbol, methodSymbol)(name).asInstanceOf[Enumeration#Value]
  }

  private def factoryMethodSymbol(enumType: Type): MethodSymbol = {
    enumType.member(newTermName("withName")).asMethod
  }

  private def reflect(module: ModuleSymbol, method: MethodSymbol)(args: Any*): Any = {
    val moduleMirror = mirror.reflectModule(module)
    val instanceMirror = mirror.reflect(moduleMirror.instance)
    instanceMirror.reflectMethod(method)(args:_*)
  }

}