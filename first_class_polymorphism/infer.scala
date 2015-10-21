package fcp

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

  def new_var(level:level):ty = TVar(Unbound(next_id(), level))
  def new_gen_var():ty = TVar(Generic(next_id()))

  def new_bound_var():(id, ty) = {
    val id = next_id()
    (id, TVar(Bound(id)))
  }
  
  def error[A](msg:String):Nothing = throw new Exception(msg)

  def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:ty) {
    def f(t:ty) {
      t match {
      case TVar(Link(ty)) => f(ty)
      case TVar(Generic(_)) | TVar(Bound(_)) =>
      case other_tvar@TVar(Unbound(other_id, other_level)) =>
          if (other_id == tvar_id) error("recursive types")
          if (other_level > tvar_level)
              other_tvar.a = Unbound(other_id, tvar_level)
      case TApp(ty, ty_arg_list) =>
          f(ty)
          ty_arg_list.foreach(f)
      case TArrow(param_ty_list, return_ty) =>
          param_ty_list.foreach(f)
          f(return_ty)
      case TForall(_, ty) => f(ty)
      case TConst(_) =>
      }
    }
    f(ty)
  }

  def substitute_bound_vars(var_id_list:List[id], ty_list:List[ty], ty:ty):ty = {
    def f(id_ty_map:Map[id,ty], ty:ty):ty = {
      ty match {
      case TConst(_) => ty
      case TVar(Link(ty)) => f(id_ty_map, ty)
      case TVar(Bound(id)) => id_ty_map.getOrElse(id, ty)
          
      case TVar(_) => ty
      case TApp(ty, ty_arg_list) =>
          TApp(f(id_ty_map, ty), ty_arg_list.map(f(id_ty_map, _)))
      case TArrow(param_ty_list, return_ty) =>
          TArrow(param_ty_list.map(f(id_ty_map, _)), f(id_ty_map, return_ty))
      case TForall(var_id_list, ty) =>
          TForall(var_id_list, f(id_ty_map -- var_id_list, ty))
      }
    }
    f(var_id_list.zip(ty_list).toMap, ty)
  }

  def free_generic_vars(ty:ty):Set[ty] = {
    var free_var_set = Set[ty]()
    def f(ty:ty) {
      ty match {
      case TConst(_) =>
      case TVar(Link(ty)) => f(ty)
      case TVar(Bound(_)) =>
      case TVar(Generic(_)) =>
          free_var_set += ty
      case TVar(Unbound(_,_)) =>
      case TApp(ty, ty_arg_list) =>
          f(ty)
          ty_arg_list.foreach(f)
      case TArrow(param_ty_list, return_ty) =>
          param_ty_list.foreach(f)
          f(return_ty)
      case TForall(_, ty) => f(ty)
      }
    }
    f(ty)
    free_var_set
  }

  def escape_check(generic_var_list:List[ty], ty1:ty, ty2:ty):Boolean = {
    val free_var_set1 = free_generic_vars(ty1)
    val free_var_set2 = free_generic_vars(ty2)
    generic_var_list.exists{
      generic_var =>
        free_var_set1.contains(generic_var) || free_var_set2.contains(generic_var)
    }      
  }

  def unify(ty1:ty, ty2:ty) {
    if (ty1 == ty2) return
    (ty1, ty2) match {
      case (TConst(name1), TConst(name2)) if (name1 == name2) => ()
      case (TApp(ty1, ty_arg_list1), TApp(ty2, ty_arg_list2)) =>
          unify(ty1, ty2)
          ty_arg_list1.zip(ty_arg_list2).foreach{case(a,b)=>unify(a,b)}
      case (TArrow(param_ty_list1, return_ty1), TArrow(param_ty_list2, return_ty2)) =>
          param_ty_list1.zip(param_ty_list2).foreach{case(a,b)=>unify(a,b)}
          unify(return_ty1, return_ty2)

      case (TVar(Link(ty1)), ty2) => unify(ty1, ty2)
      case (ty1, TVar(Link(ty2))) => unify(ty1, ty2)

      case (TVar(Unbound(id1, _)), TVar(Unbound(id2, _))) if (id1 == id2) => assert(false)

      case (TVar(Generic(id1)), TVar(Generic(id2))) if (id1 == id2) =>
          /* This should be handled by the `ty1 == ty2` case, as there should
             be only a single instance of a particular variable. */
          assert(false)
      case (TVar(Bound(_)), _) | (_, TVar(Bound(_))) =>
          // Bound vars should have been instantiated.
          assert(false)
      case (tvar@TVar(Unbound(id, level)), ty) =>
          occurs_check_adjust_levels(id, level, ty)
          tvar.a = Link(ty)
      case (ty, tvar@TVar(Unbound(id, level))) =>
          occurs_check_adjust_levels(id, level, ty)
          tvar.a = Link(ty)
      case (forall_ty1@TForall(var_id_list1, ty1), forall_ty2@TForall(var_id_list2, ty2)) =>
          val l1 = var_id_list1.length
          val l2 = var_id_list2.length
          if(l1 != l2)
              error ("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))

          val generic_var_list = (for(i <- 0 until l1) yield { new_gen_var() }).toList
          val generic_ty1 = substitute_bound_vars(var_id_list1, generic_var_list, ty1)
          val generic_ty2 = substitute_bound_vars(var_id_list2, generic_var_list, ty2)
          unify(generic_ty1, generic_ty2)
          if (escape_check(generic_var_list, forall_ty1, forall_ty2))
            error ("cannot unify types " + string_of_ty(forall_ty1) + " and " + string_of_ty(forall_ty2))
      case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
    }
  }

  def substitute_with_new_vars(level:level, var_id_list:List[id], ty:ty):(List[ty], ty) = {
    val var_list = var_id_list.map{ case _ => new_var(level) }
    (var_list, substitute_bound_vars (var_id_list, var_list, ty))
  }

  def instantiate_ty_ann(level:level,v:(List[id], ty)):(List[ty], ty) = {
    v match {
    case (List(), ty) => (List(), ty)
    case (var_id_list, ty) => substitute_with_new_vars(level, var_id_list, ty)
    }
  }

  def instantiate(level:level, ty:ty):ty = {
    ty match {
    case TForall(var_id_list, ty) =>
        val (var_list, instantiated_ty) = substitute_with_new_vars(level, var_id_list, ty)
        instantiated_ty
    case TVar(Link(ty)) => instantiate(level, ty)
    case ty => ty
    }
  }

  def subsume(level:level, ty1:ty, ty2:ty) {
    val instantiated_ty2 = instantiate(level, ty2)
    unlink(ty1) match {
      case forall_ty1@TForall(var_id_list1, ty1) =>
          val generic_var_list = var_id_list1.map( _ => new_gen_var())
          val generic_ty1 = substitute_bound_vars(var_id_list1, generic_var_list, ty1)
          unify(generic_ty1, instantiated_ty2)
          if (escape_check(generic_var_list, forall_ty1, ty2))
            error ("type " + string_of_ty(ty2) + " is not an instance of " + string_of_ty(forall_ty1))
      case ty1 => unify(ty1, instantiated_ty2)
    }
  }

  def generalize(level:level, ty:ty):ty = {
    var var_id_list_rev_ref = List[id]()
    def f(ty:ty) {
      ty match {
      case TVar(Link(ty)) => f(ty)
      case TVar(Generic(_)) => assert(false)
      case TVar(Bound(_)) =>
      case other_tvar@TVar(Unbound(other_id, other_level)) if (other_level > level) =>
          other_tvar.a = Bound(other_id)
          if (!var_id_list_rev_ref.contains(other_id)) {
            var_id_list_rev_ref = other_id :: var_id_list_rev_ref
          }
      case TVar(Unbound(_,_)) =>
      case TApp(ty, ty_arg_list) =>
          f(ty)
          ty_arg_list.foreach(f)
      case TArrow(param_ty_list, return_ty) =>
          param_ty_list.foreach(f)
          f(return_ty)
      case TForall(_, ty) => f(ty)
      case TConst(_) =>
      }
    }
    f(ty)
    var_id_list_rev_ref match {
      case List() => ty
      case var_id_list_rev => TForall(var_id_list_rev.reverse, ty)
    }
  }

  def match_fun_ty (num_params:Int, ty:ty):(List[ty], ty) = {
    ty match {
    case TArrow(param_ty_list, return_ty) =>
        if (param_ty_list.length != num_params)
          error("unexpected number of arguments")
        else
          (param_ty_list, return_ty)
    case TVar(Link(ty)) => match_fun_ty(num_params, ty)
    case tvar@TVar(Unbound(id, level)) =>
        val param_ty_list = List.fill(num_params){new_var(level)}
        val return_ty = new_var(level)
        tvar.a = Link(TArrow(param_ty_list, return_ty))
        (param_ty_list, return_ty)
    case _ => error("expected a function")
    }
  }

  def infer(env:Map[String,ty], level:level, expr:expr):ty = {
    expr match {
    case Var(name) =>
        env.get(name) match {
          case Some(a) => a
          case None => error ("variable " + name + " not found")
        }
    case Fun(param_list, body_expr) =>
        var fn_env_ref = env
        var var_list_ref = List[ty]()
        val param_ty_list = param_list.map {
          case (param_name, maybe_param_ty_ann) =>
            val param_ty = maybe_param_ty_ann match {
              case None => // equivalent to `some[a] a`
                  val v = new_var(level + 1)
                  var_list_ref = v :: var_list_ref
                  v
              case Some(ty_ann) =>
                  val (var_list, ty) = instantiate_ty_ann(level + 1, ty_ann)
                  var_list_ref = var_list ::: var_list_ref
                  ty
            }
            fn_env_ref = fn_env_ref + (param_name -> param_ty)
            param_ty
          }
        
        val inferred_return_ty = infer(fn_env_ref, level + 1, body_expr)
        val return_ty =
          if (is_annotated(body_expr)) inferred_return_ty
          else instantiate (level + 1, inferred_return_ty)
        if (!var_list_ref.forall(is_monomorphic))
          error ("polymorphic parameter inferred: "
            + var_list_ref.map(string_of_ty).mkString(", "))
        else
          generalize(level, TArrow(param_ty_list, return_ty))
    case Let(var_name, value_expr, body_expr) =>
        val var_ty = infer(env, level + 1, value_expr)
        infer(env + (var_name -> var_ty), level, body_expr)
    case Call(fn_expr, arg_list) =>
        val fn_ty = instantiate(level + 1, infer(env, level + 1, fn_expr))
        val (param_ty_list, return_ty) = match_fun_ty(arg_list.length, fn_ty)
        infer_args(env, level + 1, param_ty_list, arg_list)
        generalize(level, instantiate(level + 1, return_ty))
    case Ann(expr, ty_ann) =>
        val (_, ty) = instantiate_ty_ann(level, ty_ann)
        val expr_ty = infer(env, level, expr)
        subsume(level, ty, expr_ty)
        ty
    }
  }

  def infer_args(env:Map[String,ty], level:level, param_ty_list:List[ty], arg_list:List[expr]) {
    
    val pair_list = param_ty_list.zip(arg_list)
    def get_ordering(ty:ty, arg:Any):Int = {
      // subsume type variables last
      unlink(ty) match {
        case TVar(Unbound(_,_)) => 1
        case _ => 0
      }
    }
    val sorted_pair_list = pair_list.sortWith{
      case ((ty1, arg1), (ty2, arg2)) =>
        get_ordering(ty1, arg1) > get_ordering(ty2, arg2)
    }
    
    sorted_pair_list.foreach {
      case (param_ty, arg_expr) =>
        val arg_ty = infer(env, level, arg_expr)
        if (is_annotated(arg_expr))
          unify(param_ty, arg_ty)
        else
          subsume(level, param_ty, arg_ty)
    }
  }
  
}

