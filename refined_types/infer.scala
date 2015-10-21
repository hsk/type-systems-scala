package refined

object Infer {
  import Expr._
  import Printing._

  // Utils
  // exception Error of string
  def error[A](msg:String):A = throw new Exception(msg)

  val current_id = Ref(0)
  def reset_id() = {
    current_id.a = 0
  }
  def next_id ():Int = {
    val id = current_id.a
    current_id.a = id + 1
    id
  }
  def new_var[A](level:level):Ty[A] = TVar(Ref(Unbound[A](next_id(), level)))
  def new_gen_var[A]():Ty[A] = TVar(Ref(Generic[A](next_id())))

  // Variable environment
  object Env {
    type env = Map[String, t_ty]

    val empty : env = Map()
    def extend(name:String, ty:t_ty, env:env):env = {
      if (env.contains(name)) error("duplicate variable name \"" + name + "\"")
      else env + (name -> ty)
    }

    def lookup(name:String, env:env):t_ty = {
      env(name)
    }

    def map[A,B](f:(A)=>B, env:Map[String, A]):Map[String,B] = {
      env.toList.foldLeft(Map[String,B]()){
        case (env, (k,v)) => env + (k -> f(v))
      }
    }

    def fold[A,B](env:Map[String, A], init:B)(f:(String, A, B)=>B):B = {
      env.foldLeft(init){
        case(b, (k, a)) =>
        f(k, a, b)
      }
    }
  }

  // Unification
  def occurs_check_adjust_levels[A](tvar_id:id, tvar_level:level, ty:Ty[A]) {
    def f(ty:Ty[A]) {
      ty match {
        case TConst(_) =>
        case TApp(name, ty_arg_list) => ty_arg_list.foreach(f)
        case TArrow(param_r_ty_list, return_r_ty) =>
          def g(r_ty:refined_ty[A]) = f(plain_ty(r_ty))
          param_r_ty_list.foreach(g)
          g(return_r_ty)
        case TVar(Ref(Generic(_))) => assert(false)
        case TVar(other_tvar @ Ref(Unbound(other_id, other_level))) =>
          if (other_id == tvar_id)
            error("recursive types")
          else if (other_level > tvar_level)
            other_tvar.a = Unbound(other_id, tvar_level)
        case TVar(Ref(Link(ty))) => f(ty)
      }
    }
    f(ty)
  }

  def unify(ty1:t_ty, ty2:t_ty) {
    if (ty1 == ty2) return
    (ty1, ty2) match {
      case (TConst(name1), TConst(name2)) if(name1 == name2) =>
      case (TApp(name1, ty_arg_list1), TApp(name2, ty_arg_list2)) if (name1 == name2) =>
          ty_arg_list1.zip(ty_arg_list2).foreach{ case (a, b)=> unify(a, b) }
      case (TArrow(param_r_ty_list1, return_r_ty1), TArrow(param_r_ty_list2, return_r_ty2)) =>
          def unify_r_ty(r_ty1:t_refined_ty, r_ty2:t_refined_ty) {
            unify(plain_ty(r_ty1), plain_ty(r_ty2))
          }
          param_r_ty_list1.zip(param_r_ty_list2).foreach{ case (a, b)=> unify_r_ty(a, b) }
          unify_r_ty(return_r_ty1, return_r_ty2)
      case (TVar(Ref(Link(ty1))), ty2) => unify(ty1, ty2)
      case (ty1, TVar(Ref(Link(ty2)))) => unify(ty1, ty2)
      case (TVar(Ref(Unbound(id1, _))), TVar(Ref(Unbound(id2, _)))) if (id1 == id2) =>
          assert(false)
          // There is only a single instance of a particular type variable.
      case (TVar(tvar@Ref(Unbound(id, level))), ty) =>
          occurs_check_adjust_levels(id, level, ty)
          tvar.a = Link(ty)
      case (ty, TVar(tvar@Ref(Unbound(id, level))) ) =>
          occurs_check_adjust_levels(id, level, ty)
          tvar.a = Link(ty)
      case (_, _) =>
        error("cannot unify types " + string_of_t_ty(ty1) + " and " + string_of_t_ty(ty2))
    }
  }


  // Type generalization and type schema instantiation
  def generalize[A](level:level, ty:Ty[A]):Ty[A] = {
    ty match {
      case TVar(Ref(Unbound(id, other_level))) if (other_level > level) =>
        TVar(Ref(Generic(id)))
      case TApp(name, arg_ty_list) => TApp(name, arg_ty_list.map(generalize(level,_)) )
      case TArrow(param_r_ty_list, return_r_ty) =>
        val g = {b:refined_ty[A]=>r_ty_map({a:Ty[A]=>generalize[A](level,a)},b)}
        TArrow(param_r_ty_list.map(g), g(return_r_ty))
      case TVar(Ref(Link(ty))) => generalize(level, ty)
      case TConst(_) => ty
      case TVar(Ref(Generic(_))) => ty
      case TVar(Ref(Unbound(_,_))) => ty
    }
  }

  def instantiate[A](level:level, ty:Ty[A]):Ty[A] = {
    var id_var_map = Map[id,Ty[A]]()
    def f(ty:Ty[A]):Ty[A] = {
      ty match {
      case TApp(name, arg_ty_list) => TApp(name, arg_ty_list.map(f))
      case TArrow(param_r_ty_list, return_r_ty) =>
        TArrow(param_r_ty_list.map(r_ty_map(f, _)), r_ty_map(f, return_r_ty))
      case TVar(Ref(Generic(id))) =>
        try {
          id_var_map(id)
        } catch {
          case _:Throwable =>
          val v = new_var[A](level)
          id_var_map = id_var_map + (id -> v)
          v
        }
      case TVar(Ref(Link(ty))) => f(ty)
      case TConst(_) => ty
      case TVar(Ref(Unbound(_,_))) => ty
      }
    }
    f(ty)
  }

  // Type inference and typed tree construction
  def match_fun_ty(num_params:Int, ty:t_ty):(List[t_refined_ty],t_refined_ty) = {
    ty match {
    case TArrow(param_r_ty_list, return_r_ty) =>
        if (param_r_ty_list.length != num_params)
          error("unexpected number of arguments")
        else
          (param_r_ty_list, return_r_ty)
    case TVar(tvar@Ref(Unbound(id, level))) =>
        val param_r_ty_list = {
          def f(n:Int):List[refined_ty[t_expr]] = {
            n match {
            case 0 => List()
            case n => Plain(new_var[t_expr](level)) :: f(n - 1)
            }
          }
          f(num_params)
        }
        
        val return_r_ty = Plain(new_var[t_expr](level))
        tvar.a = Link(TArrow(param_r_ty_list, return_r_ty))
        (param_r_ty_list, return_r_ty)
    case TVar(Ref(Link(ty))) => match_fun_ty(num_params, ty)
    case _ => error("expected a function")
    }
  }

  def infer_expr(env:Env.env, level:level, s:s_expr):t_expr = {
    s match {
      case SVar(name) =>
        val ty =
          try {
            instantiate(level, Env.lookup(name, env))
          } catch {
            case e:Throwable =>
              error("variable " + name + " not found")
          }
        t_expr(EVar(name), ty)
      case SBool(b) => t_expr(EBool(b), t_bool)
      case SInt(i) => t_expr(EInt(i), TConst("int"))
      case SCall(fn_s_expr, arg_s_expr_list) =>
        val fn_t_expr = infer_expr(env, level, fn_s_expr)
        val (param_r_ty_list, return_r_ty) =
          match_fun_ty(arg_s_expr_list.length, fn_t_expr.ty)
        
        val arg_t_expr_list =
          param_r_ty_list.zip(arg_s_expr_list).map {
          case (param_r_ty, arg_s_expr) =>
            val arg_t_expr = infer_expr(env, level, arg_s_expr)
            unify(plain_ty(param_r_ty), arg_t_expr.ty)
            arg_t_expr
          }
        t_expr(
          ECall(fn_t_expr, arg_t_expr_list),
          plain_ty(return_r_ty))
      case SFun(s_param_list, maybe_return_s_r_ty, body_s_expr) =>
        infer_function(env, level, s_param_list, maybe_return_s_r_ty, body_s_expr)
      case SLet(var_name, value_s_expr, body_s_expr) =>
        val value_t_expr = infer_expr(env, level + 1, value_s_expr)
        val generalized_ty = generalize(level, value_t_expr.ty)
        val new_env = Env.extend(var_name, generalized_ty, env)
        val body_t_expr = infer_expr(new_env, level, body_s_expr)
        t_expr(
          ELet(var_name, t_expr(value_t_expr.shape, generalized_ty), body_t_expr),
          body_t_expr.ty
        )
      case SIf(cond_s_expr, then_s_expr, else_s_expr) =>
        val cond_t_expr = infer_expr(env, level, cond_s_expr)
        unify(cond_t_expr.ty, t_bool)
        val then_t_expr = infer_expr(env, level, then_s_expr)
        val else_t_expr = infer_expr(env, level, else_s_expr)
        unify(then_t_expr.ty, else_t_expr.ty)
        t_expr(
          EIf(cond_t_expr, then_t_expr, else_t_expr),
          then_t_expr.ty
        )
      case SCast(s_expr, s_ty, maybe_contract_s_expr) =>
        // Equivalent to: `(fun (x : ty if contract) -> x)(expr)`.
        val t_expr1 = infer_expr(env, level, s_expr)
        val instantiated_t_ty = instantiate_and_infer_ty(env, level, s_ty)
        val plain_t_ty = strip_refined_types(instantiated_t_ty)
        unify(plain_t_ty, t_expr1.ty)
        val maybe_contract_t_expr = maybe_contract_s_expr match {
          case None => None
          case Some(contract_s_expr) =>
            Some (infer_contract(env, level, contract_s_expr))
        }
        t_expr(
          ECast(t_expr1, instantiated_t_ty, maybe_contract_t_expr),
          plain_t_ty
        )
    }
  }

  def instantiate_and_infer_ty(env:Env.env, level:level, ty:s_ty):t_ty = {
    infer_ty(env, level, instantiate(level, ty))
  }

  def infer_ty(env:Env.env, level:level, ty:Ty[s_expr]):t_ty = {
    ty match {
      // Transforms a s_ty into a t_ty, infering the types of contracts along the way.
      case TConst(name) => TConst(name)
      case TApp(name, arg_ty_list) => TApp(name, arg_ty_list.map(infer_ty(env, level, _)) )
      case TArrow(param_s_r_ty_list, return_s_r_ty) =>
        val (new_env, rev_param_t_r_ty_list) = param_s_r_ty_list.foldLeft((env, List[refined_ty[t_expr]]())) {
          case ((env, rev_param_t_r_ty_list), param_s_r_ty) =>
            val (new_env, param_t_r_ty) = infer_r_ty(env, level, param_s_r_ty)
            (new_env, param_t_r_ty :: rev_param_t_r_ty_list)
        }
        
        val param_t_r_ty_list = rev_param_t_r_ty_list.reverse
        val (_, return_t_r_ty) = infer_r_ty(new_env, level, return_s_r_ty)
        TArrow(param_t_r_ty_list, return_t_r_ty)
      case TVar(Ref(Unbound(id, level))) => TVar(Ref(Unbound(id, level)))
      case TVar(Ref(Generic(id))) => TVar(Ref(Generic(id)))
      case TVar(Ref(Link(ty))) => infer_ty(env, level, ty)
    }
  }

  def infer_contract(env:Env.env, level:level, s_expr:s_expr):t_expr = {
    val t_expr = infer_expr(env, level, s_expr)
    unify(t_expr.ty, t_bool)
    t_expr
  }

  def infer_r_ty(env:Env.env, level:level, r:refined_ty[s_expr]):(Env.env,refined_ty[t_expr]) = {
    r match {
      case Plain(s_ty) => (env, Plain(infer_ty(env, level, s_ty)))
      case Named(name, s_ty) =>
        val t_ty = infer_ty(env, level, s_ty)
        val new_env = Env.extend(name, strip_refined_types(t_ty), env)
        (new_env, Named(name, t_ty))
      case Refined(name, s_ty, s_expr) =>
        val t_ty = infer_ty(env, level, s_ty)
        val new_env = Env.extend(name, strip_refined_types(t_ty), env)
        val t_expr = infer_contract(new_env, level, s_expr)
        (new_env, Refined(name, t_ty, t_expr))
    }
  }

  def infer_function(env:Env.env, level:level, s_param_list:List[s_param],
    maybe_return_s_r_ty:Option[s_refined_ty], body_s_expr:s_expr):t_expr = {
    val (new_env:Env.env, rev_t_param_list, rev_param_t_r_ty_list) =
      s_param_list.foldLeft((env, List[t_param](), List[Plain[t_expr]]())) {
        case ((env, rev_t_param_list, rev_param_t_r_ty_list), s_param1) =>
          val (new_env, t_param1, param_t_ty) =
            s_param1 match {
              case s_param(param_name, None) =>
                val param_t_ty = new_var[t_expr](level)
                val new_env = Env.extend(param_name, param_t_ty, env)
                (new_env, t_param(param_name, param_t_ty, None), param_t_ty)
              case s_param(param_name, Some((param_s_ty, None))) =>
                val param_t_ty = instantiate_and_infer_ty(env, level, param_s_ty)
                val new_env = Env.extend(param_name, strip_refined_types(param_t_ty), env)
                (new_env, t_param(param_name, param_t_ty, None), param_t_ty)
              case s_param(param_name, Some((param_s_ty, Some(contract_s_expr)))) =>
                val param_t_ty = instantiate_and_infer_ty(env, level, param_s_ty)
                val new_env = Env.extend(param_name, strip_refined_types(param_t_ty), env)
                val contract_t_expr = infer_contract(new_env, level, contract_s_expr)
                (new_env, t_param(param_name, param_t_ty, Some(contract_t_expr)), param_t_ty)
            }
          (new_env, t_param1 :: rev_t_param_list, Plain(param_t_ty) :: rev_param_t_r_ty_list)
      }
    val t_param_list = rev_t_param_list.reverse
    val param_t_r_ty_list = rev_param_t_r_ty_list.reverse
    val body_t_expr = infer_expr(new_env, level, body_s_expr)
    val maybe_return_t_r_ty =
      maybe_return_s_r_ty match {
        case None => None
        case Some(s_r_ty) =>
          val s_ty = plain_ty(s_r_ty)
          val instantiated_t_ty = instantiate_and_infer_ty(new_env, level, s_ty)
          val plain_t_ty = strip_refined_types(instantiated_t_ty)
          unify(plain_t_ty, body_t_expr.ty)
          val t_r_ty = s_r_ty match {
            case Plain(_) => Plain(instantiated_t_ty)
            case Named(_,_) => Plain(instantiated_t_ty)
            case Refined(name, _, s_expr) =>
              val return_ty_env = Env.extend(name, plain_t_ty, new_env)
              val t_expr = infer_contract(return_ty_env, level, s_expr)
              Refined(name, instantiated_t_ty, t_expr)
          }
          Some(t_r_ty)
      }

    t_expr(
      EFun(t_param_list, maybe_return_t_r_ty, body_t_expr),
      TArrow(param_t_r_ty_list, Plain(body_t_expr.ty))
    )
  }
  
}
