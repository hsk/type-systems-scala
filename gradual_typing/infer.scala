package dhm

object Infer {
  import Expr._

  case class Settings(
    var dynamic_parameters : Boolean,
    var freeze_dynamic : Boolean
  )
  
  val settings = Settings(
    dynamic_parameters = true,
    freeze_dynamic = true
  )

  val current_id = Ref(0)

  def next_id():Int = {
    val id = current_id.a
    current_id.a = id + 1
    id
  }

  def reset_id() {
    current_id.a = 0
  }

  def new_var(level:level, is_dynamic:Boolean):Ty =
    TVar(Ref(Unbound(next_id(), level, is_dynamic)))

  def new_gen_var():Ty = TVar(Ref(Generic(next_id())))

  def error[A](msg:String):A = { throw new Exception(msg) }

  object Env {
    type env = Map[String,Ty]

    val empty : env = Map()
    def extend (env:env, name:String, ty:Ty):env = env + (name -> ty)
    def lookup (env:env, name:String):Ty = env(name)
  }

  def occurs_check_adjust_levels_make_vars_dynamic(tvar_id:id, tvar_level:level, tvar_is_dynamic:Boolean, ty:Ty) {
    def f(ty:Ty) {
      ty match {
        case TVar(Ref(Link(ty))) => f(ty)
        case TVar(Ref(Generic(_))) => assert(false)
        case TVar(other_tvar @ Ref(Unbound(other_id, other_level, other_is_dynamic))) =>
          if (other_id == tvar_id) error("recursive types")          
          val new_level = Math.min(tvar_level, other_level)
          val new_is_dynamic = tvar_is_dynamic || other_is_dynamic
          other_tvar.a = Unbound(other_id, new_level, new_is_dynamic)
        case TApp(ty, ty_arg_list) =>
          f(ty)
          ty_arg_list.foreach(f)
        case TArrow(param_ty_list, return_ty) =>
          param_ty_list.foreach(f)
          f(return_ty)
        case TConst(_) =>
        case TDynamic => assert(false)
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
      case (TVar(Ref(Link(ty1))), ty2) => unify(ty1, ty2)
      case (ty1, TVar(Ref(Link(ty2)))) => unify(ty1, ty2)
      case (TVar(Ref(Unbound(id1, _, _))), TVar(Ref(Unbound(id2, _, _)))) if id1 == id2 =>
          assert(false) // There is only a single instance of a particular type variable.
      case (TVar(tvar @ Ref(Unbound(id, level, is_dynamic))), ty) =>
          occurs_check_adjust_levels_make_vars_dynamic(id, level, is_dynamic, ty)
          tvar.a = Link(ty)
      case (ty, TVar(tvar @ Ref(Unbound(id, level, is_dynamic)))) =>
          occurs_check_adjust_levels_make_vars_dynamic(id, level, is_dynamic, ty)
          tvar.a = Link(ty)
      case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
    }
  }

  def generalize(level:level, ty:Ty):Ty = {
    ty match {
      case TDynamic => error("assert")
      case TVar(Ref(Unbound(id, other_level, is_dynamic))) if other_level > level =>
        if (is_dynamic) {
          if (settings.freeze_dynamic)
            TDynamic
          else
            TVar(Ref(Unbound(id, level, true)))
        } else
          TVar(Ref(Generic(id)))
      case TApp(ty, ty_arg_list) =>
        TApp(generalize(level, ty), ty_arg_list.map(generalize(level, _)))
      case TArrow(param_ty_list, return_ty) =>
        TArrow(param_ty_list.map(generalize(level, _)), generalize(level, return_ty))
      case TVar(Ref(Link(ty))) => generalize(level, ty)
      case TVar(Ref(Generic(_))) | TVar(Ref(Unbound(_, _, _))) | TConst(_) => ty
    }
  }

  def instantiate_helper(instantiate_dynamic: Boolean, level:level, ty:Ty):Ty = {
    var id_var_map = Map[id,Ty]()
    def f (ty:Ty):Ty = {
      ty match {
        case TConst(_) => ty
        case TVar(Ref(Link(ty))) => f(ty)
        case TVar(Ref(Generic(id))) =>
          try {
            id_var_map(id)
          } catch {
            case _:Throwable =>
              val var1 = new_var(level, false)
              id_var_map = id_var_map + (id -> var1)
              var1
          }
        case TVar(Ref(Unbound(_,_,_))) => ty
        case TDynamic =>
          if (instantiate_dynamic)
            new_var(level, true)
          else
            TDynamic
        case TApp(ty, ty_arg_list) =>
          TApp(f(ty), ty_arg_list.map(f))
        case TArrow(param_ty_list, return_ty) =>
          TArrow(param_ty_list.map(f), f(return_ty))
      }
    }
    f(ty)
  }

  def instantiate(level:level, ty:Ty):Ty = instantiate_helper(true, level, ty)

  def instantiate_ty_ann(level:level, ty:Ty):Ty = instantiate_helper(false, level, ty)

  def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {
    ty match {
      case TArrow(param_ty_list, return_ty) =>
        if (param_ty_list.length != num_params)
          throw new Exception("unexpected number of arguments")
        (param_ty_list, return_ty)
      case TVar(Ref(Link(ty))) => match_fun_ty(num_params, ty)
      case TVar(tvar@Ref(Unbound(id, level, is_dynamic))) =>
        val param_ty_list = { 
          def f(n:Int):List[Ty] = {
            n match {
              case 0 => List()
              case n => new_var(level, is_dynamic) :: f(n - 1)
            }
          }
          f(num_params)
        }
        val return_ty = new_var(level, is_dynamic)
        tvar.a = Link(TArrow(param_ty_list, return_ty))
        (param_ty_list, return_ty)
      case TDynamic => error("assert")
      case _ => throw new Exception("expected a function")
    }
  }


  def duplicate_dynamic(level:level, ty:Ty):Ty = {
    ty match {
    case TDynamic => error("assert")
    case TVar(Ref(Unbound(id, other_level, true))) if other_level > level =>
        new_var(level, true)
    case TApp(ty, ty_arg_list) =>
        TApp(duplicate_dynamic(level, ty), ty_arg_list.map(duplicate_dynamic(level, _)))
    case TArrow(param_ty_list, return_ty) =>
        TArrow(param_ty_list.map(duplicate_dynamic(level, _)), duplicate_dynamic(level, return_ty))
    case TVar(Ref(Link(ty))) => duplicate_dynamic(level, ty)
    case TVar(Ref(Generic(_))) | TVar(Ref(Unbound(_,_,_))) | TConst(_) => ty
    }
  }

  def infer(env:Env.env, level:level, expr:Expr):Ty = {
    expr match {
      case Var(name) =>
        try {
          instantiate(level, (Env.lookup(env, name)))
        } catch {
          case _:Throwable => throw new Exception("variable " + name + " not found")
        }
      case Fun(param_list, body_expr) =>
        val fn_env_ref = Ref(env)
        val param_ty_list = param_list.map{
          case (param_name, maybe_param_ty_ann) =>
            val param_ty = maybe_param_ty_ann match {
              case None =>
                if (settings.dynamic_parameters) TDynamic
                else new_var(level, false)
              case Some(ty_ann) =>
                instantiate_ty_ann(level, ty_ann)
            }
            fn_env_ref.a = Env.extend(fn_env_ref.a, param_name, param_ty)
            param_ty
        }
        val return_ty = infer(fn_env_ref.a, level, body_expr)
        TArrow(param_ty_list.map(instantiate(level, _)), return_ty)
      case Let(var_name, None, value_expr, body_expr) =>
        val var_ty = infer(env, level + 1, value_expr)
        val generalized_ty = generalize(level, var_ty)
        infer (Env.extend(env, var_name, generalized_ty), level, body_expr)
      case Let(var_name, Some(ty_ann), value_expr, body_expr) =>
        // equivalent to `let var_name = (value_expr : ty_ann) in body_expr`
        infer(env, level, Let(var_name, None, Ann(value_expr, ty_ann), body_expr))
      case Call(fn_expr, arg_list) =>
        val (param_ty_list, return_ty) =
          match_fun_ty(arg_list.length, infer(env, level + 1, fn_expr))
        param_ty_list.zip(arg_list).foreach{
          case (param_ty, arg_expr) => unify (param_ty, infer(env, level + 1, arg_expr))
        } 
        duplicate_dynamic(level, return_ty)
      case Ann(expr, ty_ann) =>
          // equivalent to `(fun (x : ty_ann) -> x)(expr)`
          infer(env, level, Call(Fun(List("x" -> Some(ty_ann)), Var("x")), List(expr)))
    }
  }
}