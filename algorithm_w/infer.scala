package dhm

object Infer {

  import Expr._
  var current_id = 0

  def next_id():Int = {
    val id = current_id
    current_id = id + 1
    id
  }

  def reset_id() {
    current_id = 0
  }

  def new_var(tvar_level:level):Ty = TVar(Unbound(next_id(), tvar_level))

  def new_gen_var():Ty = TVar(Generic(next_id()))

  def error(msg:String):Nothing = throw new Exception(msg)

  def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:Ty) {
    def f(ty:Ty) {
      ty match {
        case TVar(Link(ty)) => f(ty)
        case TVar(Generic(_)) => assert(false)
        case other_tvar @ TVar(Unbound(other_id, other_level)) =>
          if (other_id == tvar_id) error("recursive types")
          if (other_level > tvar_level)
            other_tvar.a = Unbound(other_id, tvar_level)
        case TApp(ty, ty_arg_list) =>
          f(ty)
          ty_arg_list.foreach(f)
        case TArrow(param_ty_list, return_ty) =>
          param_ty_list.foreach(f)
          f(return_ty)
        case TConst(_) =>
      }
    }
    f(ty)
  }

  def unify(ty1:Ty, ty2:Ty) {
    if (ty1 == ty2) return
    (ty1, ty2) match {
      case (TConst(name1), TConst(name2)) if(name1 == name2) =>
      case (TApp(ty1, ty_arg_list1), TApp(ty2, ty_arg_list2)) =>
          ty_arg_list1.zip(ty_arg_list2).foreach{
            case (a,b) => unify(a, b)
          }
      case (TArrow(param_ty_list1, return_ty1), TArrow(param_ty_list2, return_ty2)) =>
          param_ty_list1.zip(param_ty_list2).foreach{
            case(a,b) => unify(a, b)
          }
          unify(return_ty1, return_ty2)
      case (TVar(Link(ty1)), ty2) => unify(ty1, ty2)
      case (ty1, TVar(Link(ty2))) => unify(ty1, ty2)
      case (TVar(Unbound(id1, _)), TVar(Unbound(id2, _))) if id1 == id2 =>
          assert(false) // There is only a single instance of a particular type variable.
      case (tvar @ TVar(Unbound(id, tvar_level)), ty) =>
          occurs_check_adjust_levels(id, tvar_level, ty)
          tvar.a = Link(ty)
      case (ty, tvar @ TVar(Unbound(id, tvar_level))) =>
          occurs_check_adjust_levels(id, tvar_level, ty)
          tvar.a = Link(ty)
      case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
    }
  }

  def generalize(level:level, ty:Ty):Ty = {
    ty match {
      case TVar(Unbound(id, other_level)) if other_level > level =>
        TVar(Generic(id))
      case TApp(ty, ty_arg_list) =>
        TApp(generalize(level, ty), ty_arg_list.map(generalize(level, _)))
      case TArrow(param_ty_list, return_ty) =>
        TArrow(param_ty_list.map(generalize(level, _)), generalize(level, return_ty))
      case TVar(Link(ty)) => generalize(level, ty)
      case TVar(Generic(_)) | TVar(Unbound(_, _)) | TConst(_) => ty
    }
  }

  def instantiate(level:level, ty:Ty):Ty = {
    var id_var_map = Map[id,Ty]()
    def f (ty:Ty):Ty = {
      ty match {
        case TConst(_) => ty
        case TVar(Link(ty)) => f(ty)
        case TVar(Generic(id)) =>
          id_var_map.get(id) match {
            case Some(a) => a
            case None =>
              val var1 = new_var(level)
              id_var_map = id_var_map + (id -> var1)
              var1
          }
        case TVar(Unbound(_,_)) => ty
        case TApp(ty, ty_arg_list) =>
          TApp(f(ty), ty_arg_list.map(f))
        case TArrow(param_ty_list, return_ty) =>
          TArrow(param_ty_list.map(f), f(return_ty))
      }
    }
    f(ty)
  }

  def infer(env:Map[String,Ty], level:level, expr:Expr):Ty = {
    expr match {
      case Var(name) =>
        try {
          instantiate(level, env(name))
        } catch {
          case _:Throwable => error("variable " + name + " not found")
        }
      case Fun(param_list, body_expr) =>
        val param_ty_list = param_list.map{ _ => new_var(level)}
        val fn_env =
          param_list.zip(param_ty_list).foldLeft(env) {
            case(env, (param_name, param_ty)) => env + (param_name -> param_ty)
          }
        val return_ty = infer(fn_env, level, body_expr)
        TArrow(param_ty_list, return_ty)
      case Let(var_name, value_expr, body_expr) =>
        val var_ty = infer(env, level + 1, value_expr)
        val generalized_ty = generalize(level, var_ty)
        infer (env + (var_name -> generalized_ty), level, body_expr)
      case Call(fn_expr, arg_list) =>
        def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {
          ty match {
            case TArrow(param_ty_list, return_ty) =>
              if (param_ty_list.length != num_params)
                error("unexpected number of arguments")
              (param_ty_list, return_ty)
            case TVar(Link(ty)) => match_fun_ty(num_params, ty)
            case tvar@TVar(Unbound(id, tvar_level)) =>
              val param_ty_list = List.fill(num_params){new_var(tvar_level)}
              val return_ty = new_var(tvar_level)
              tvar.a = Link(TArrow(param_ty_list, return_ty))
              (param_ty_list, return_ty)
            case _ => error("expected a function")
          }
        }
        val (param_ty_list, return_ty) =
          match_fun_ty(arg_list.length, infer(env, level, fn_expr))
        param_ty_list.zip(arg_list).foreach{
          case (param_ty, arg_expr) => unify (param_ty, infer(env, level, arg_expr))
        } 
        return_ty
    }
  }
}