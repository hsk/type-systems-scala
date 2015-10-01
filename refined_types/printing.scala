package refined

object Printing {
  import Expr._

  // Utils
  def string_set_from_list[A](l:List[A]) = l.toSet
  def hashtbl_values[A,B](h:Map[A,B]):List[B] = h.values.toList

  // Operators
  val binary_operators =
    string_set_from_list(List("and", "or", "<", ">", "<=", ">=", "==", "!=", "+", "-", "*", "/", "%"))

  val unary_operators =
    string_set_from_list(List("not", "unary-"))

  // Printing types
  def string_of_ty_with_var_names[A](string_of_expr:A=>String, ty:Ty[A]):(List[String], String) = {
    var id_name_map = Map[id,String]()
    val count = Ref(0)
    
    def next_name():String = {
      val i = count.a
      count.a += 1
      (97 + i % 26).toChar.toString + (if (i >= 26) ""+(i / 26) else "")
    }
    def complex_ty(ty:Ty[A]):String = {
      ty match {
        case TArrow(param_r_ty_list, return_r_ty) =>
          def string_of_param_r_ty(rt:refined_ty[A]):String = {
            rt match {
              case Plain(ty) => simple_ty(ty)
              case Named(name, ty) => name + " : " + complex_ty(ty)
              case Refined(name, ty, expr) =>
                name + " : " + complex_ty(ty) + " if " + string_of_expr(expr)
            }
          }
          val param_r_ty_list_str = param_r_ty_list match {
            case List(Plain(ty)) => simple_ty(ty)
            case _ => "(" + param_r_ty_list.map(string_of_param_r_ty).mkString(", ") + ")"
          }
          val return_r_ty_str = return_r_ty match {
            case Plain(ty) => complex_ty(ty)
            case Named(name, ty) => "(" + name + " : " + complex_ty(ty) + ")"
            case Refined(name, ty, expr) =>
                "(" + name + " : " + complex_ty(ty) + " if " + string_of_expr(expr) + ")"
          }
          param_r_ty_list_str + " -> " + return_r_ty_str
        case TVar(Ref(Link(ty))) => complex_ty(ty)
        case ty => simple_ty(ty)
      }
    }
    
    def simple_ty(ty:Ty[A]):String = {
      ty match {
        case TConst(name) => name
        case TApp(name, arg_ty_list) =>
          name + "[" + arg_ty_list.map(complex_ty).mkString(", ") + "]"
        case TVar(Ref(Generic(id))) =>
          try {
            id_name_map(id)
          } catch {
            case _:Throwable =>
              val name = next_name()
              id_name_map = id_name_map + (id -> name)
              name
          }
        case TVar(Ref(Unbound(id, _))) =>
          "@unknown" + id
        case TVar(Ref(Link(ty))) => simple_ty(ty)
        case ty => "(" + complex_ty(ty) + ")"
      }
    }
    val ty_str = complex_ty(ty)
    if (count.a > 0) {
      val var_names = hashtbl_values(id_name_map)
      (var_names.sorted, ty_str)
    } else {
      (List(), ty_str)
    }
  }

  def string_of_ty[A](string_of_expr:A=>String, ty:Ty[A]) : String = {
    val (var_names, ty_str) = string_of_ty_with_var_names(string_of_expr, ty)
    var_names match {
      case List() => ty_str
      case var_names => "forall[" + var_names.mkString(" ") + "] " + ty_str
    }
  }

  def string_of_ty_ann[A](string_of_expr:A=>String, ty:Ty[A]) : String = {
    val (var_names, ty_str) = string_of_ty_with_var_names(string_of_expr, ty)
    var_names match {
      case List() => ty_str
      case var_names => "some[" + var_names.mkString(" ") + "] " + ty_str
    }
  }

  // Printing syntax trees
  def string_of_s_ty(ty:s_ty):String = string_of_ty(string_of_s_expr, ty)

  def string_of_s_ty_ann(ty:s_ty):String = string_of_ty_ann(string_of_s_expr, ty)

  def string_of_s_expr(expr:s_expr): String = {
    def complex_expr(s:s_expr):String = {
      s match {
        case SFun(param_list, maybe_return_r_ty, body_expr) =>
          val param_list_str = param_list.map {
            case s_param(name, None) => name
            case s_param(name, Some((ty, None))) => name + " : " + string_of_s_ty_ann(ty)
            case s_param(name, Some((ty, Some(contract_expr)))) =>
              name + " : " + string_of_s_ty_ann(ty) + " if " + complex_expr(contract_expr)
          }.mkString(", ")
          val return_r_ty_str =
            maybe_return_r_ty match {
              case None => ""
              case Some(return_r_ty) =>
                " : " + (return_r_ty match {
                  case Plain(ty) =>
                    if (is_function_ty(ty))
                      "(" + string_of_s_ty_ann(ty) + ")"
                    else
                      string_of_s_ty_ann(ty)
                  case Named(name, ty) => "(" + name + " : " + string_of_s_ty_ann(ty) + ")"
                  case Refined(name, ty, expr) =>
                    "(" + name + " : " + string_of_s_ty_ann(ty) + " if " + string_of_s_expr(expr) + ")"
                })
            }
          "fun(" + param_list_str + ")" + return_r_ty_str + " -> " + complex_expr(body_expr)
        case SLet(var_name, value_expr, body_expr) =>
          "let " + var_name + " = " + complex_expr(value_expr) +
          " in " + complex_expr(body_expr)
        case SIf(cond_expr, then_expr, else_expr) =>
          "if " + complex_expr(cond_expr) +
          " then " + complex_expr(then_expr) +
          " else " + complex_expr(else_expr)
        case SCast(expr, ty, None) => simple_expr(expr) + " : " + string_of_s_ty_ann(ty)
        case SCast(expr, ty, Some(contract_expr)) =>
          simple_expr(expr) + " : " + string_of_s_ty_ann(ty) +
          " if " + complex_expr(contract_expr)
        case expr => simple_expr(expr)
      }
    }

    def simple_expr(s:s_expr):String = {
      s match {
        case SVar(name) => name
        case SBool(b) => b.toString
        case SInt(i) => i.toString
        case SCall(SVar(op), arg_expr_list) if (binary_operators.contains(op)) =>
          arg_expr_list match {
            case List(left_expr, right_expr) =>
              "(" + simple_expr(left_expr) + " " + op + " " + simple_expr(right_expr) + ")"
            case _ => throw new Exception("binary op " + op + " expects 2 arguments")
          }
        case SCall(SVar(op), arg_expr_list) if (unary_operators.contains(op)) =>
          arg_expr_list match {
            case List(expr) => "(" + (if (op == "unary-") "-" else op) + " " + simple_expr(expr) + ")"
            case _ => throw new Exception("unary op " + op + " expects a single argument")
          }
        case SCall(fn_expr, arg_expr_list) =>
          simple_expr(fn_expr) + "(" + arg_expr_list.map(complex_expr).mkString(", ") + ")"
        case expr => "(" + complex_expr(expr) + ")"
      }
    }
    
    complex_expr(expr)
  }

  // Printing typed expressions

  // def string_of_plain_t_ty(ty) = string_of_s_ty(duplicate_without_refined_types(ty))

  def string_of_t_ty(ty:t_ty):String = string_of_ty(string_of_t_expr, ty)

  def string_of_t_ty_ann(ty:t_ty):String = string_of_ty_ann(string_of_t_expr, ty)

  def string_of_t_expr(expr:t_expr) : String = {
    def complex_expr(expr:t_expr):String = expr.shape match {
      case EFun(param_list, maybe_return_r_ty, body_expr) =>
          val param_list_str = param_list.map {
            case t_param(name, ty, None) => name + " : " + string_of_t_ty_ann(ty)
            case t_param(name, ty, Some(contract_expr)) =>
                name + " : " + string_of_t_ty_ann(ty) + " if " + complex_expr(contract_expr)
          }.mkString(", ")
          val return_r_ty_str = maybe_return_r_ty match {
            case None => ""
            case Some(return_r_ty) =>
              " : " + (return_r_ty match {
                case Plain(ty) =>
                  if (is_function_ty(ty))
                    "(" + string_of_t_ty_ann(ty) + ")"
                  else
                    string_of_t_ty_ann(ty)
                case Named(name, ty) => "(" + name + " : " + string_of_t_ty_ann(ty) + ")"
                case Refined(name, ty, expr) =>
                    "(" + name + " : " + string_of_t_ty_ann(ty) +
                    " if " + string_of_t_expr(expr) + ")"
              })
          }
          "fun(" + param_list_str + ")" + return_r_ty_str + " -> " + complex_expr(body_expr)
      case ELet(var_name, value_expr, body_expr) =>
          "let " + var_name + " = " + complex_expr(value_expr) + " in " + complex_expr(body_expr)
      case EIf(cond_expr, then_expr, else_expr) =>
          "if " + complex_expr(cond_expr) +
          " then " + complex_expr(then_expr) +
          " else " + complex_expr(else_expr)
      case ECast(expr, ty, None) => simple_expr(expr) + " : " + string_of_t_ty_ann(ty)
      case ECast(expr, ty, Some(contract_expr)) =>
          simple_expr(expr) + " : " + string_of_t_ty_ann(ty) + " if " + complex_expr(contract_expr)
      case _ => simple_expr(expr)
    }

    def simple_expr(expr:t_expr):String = {
      expr.shape match {
        case EVar(name) => name
        case EBool(b) => b.toString
        case EInt(i) => i.toString
        case ECall(t_expr(EVar(op), _), arg_expr_list) if (binary_operators.contains(op)) =>
          arg_expr_list match {
            case List(left_expr, right_expr) =>
              "(" + simple_expr(left_expr) + " " + op + " " + simple_expr(right_expr) + ")"
            case _ => throw new Exception("binary op " + op + " expects 2 arguments")
          }
        case ECall(t_expr(EVar(op), _), arg_expr_list) if (unary_operators.contains(op)) =>
          arg_expr_list match {
            case List(expr) => "(" + (if (op == "unary-") "-" else op) + " " + simple_expr(expr) + ")"
            case _ => throw new Exception("unary op " + op + " expects a single argument")
          }
        case ECall(fn_expr, arg_expr_list) =>
          simple_expr(fn_expr) + "(" + arg_expr_list.map(complex_expr).mkString(", ") + ")"
        case _ => "(" + complex_expr(expr) + ")"
      }
    }

    complex_expr(expr)
  }
  

}