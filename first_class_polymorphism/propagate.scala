package fcp
/*
  Parts of this file are based on code from Daan Leijen's
  reference implementation of HMF, available at (as of 2014/04/04)
  http://research.microsoft.com/en-us/um/people/daan/download/hmf-prototype-ref.zip
*/

object Propagate {

  import Expr._
  import Infer._

  sealed trait generalized
  case object Generalized extends generalized
  case object Instantiated extends generalized


  def should_generalize(expected_ty:ty):generalized = {
    expected_ty match {
    case TForall(_,_) => Generalized
    case TVar(Ref(Link(ty))) => should_generalize(ty)
    case _ => Instantiated
    }
  }

  def maybe_generalize(generalized:generalized, level:level, ty:ty):ty = {
    generalized match {
    case Instantiated => ty
    case Generalized => generalize(level, ty)
    }
  }

  def maybe_instantiate(generalized:generalized, level:level, ty:ty):ty = {
    generalized match {
    case Instantiated => instantiate(level, ty)
    case Generalized => ty
    }
  }

  def generalize_or_instantiate(generalized:generalized, level:level, ty:ty):ty = {
    generalized match {
    case Instantiated => instantiate(level, ty)
    case Generalized => generalize(level, ty)
    }
  }

  def infer(env:Map[String,ty], level:level, maybe_expected_ty:Option[ty], generalized:generalized, e:expr):ty = {
    e match {
    case Var(name) =>
        try {
          maybe_instantiate(generalized, level, env(name))
        } catch {
          case _:Throwable => 
            error ("variable " + name + " not found")
        }
    case Fun(param_list, body_expr) =>
        val (expected_param_list, maybe_expected_return_ty, body_generalized) =
          maybe_expected_ty match {
            case None => (param_list, None, Instantiated)
            case Some(expected_ty) =>
                 instantiate(level + 1, expected_ty) match {
                  case TArrow(expected_param_ty_list, expected_return_ty) =>
                      (param_list.zip(expected_param_ty_list).map {
                        case ((param_name, maybe_param_ty_ann), expected_param_ty) =>
                          (param_name,
                            if(maybe_param_ty_ann == None)
                              Some((List(), expected_param_ty))
                            else maybe_param_ty_ann)
                      }, Some(expected_return_ty), should_generalize(expected_return_ty))
                  case _ => (param_list, None, Instantiated)
                }
          }
        

        val fn_env_ref = Ref(env)
        val var_list_ref = Ref(List[ty]())
        val param_ty_list = expected_param_list.map {
          case (param_name, maybe_param_ty_ann) =>
            val param_ty = maybe_param_ty_ann match {
              case None => // equivalent to `some[a] a`
                  val v = new_var(level + 1)
                  var_list_ref.a = v :: var_list_ref.a
                  v
              case Some(ty_ann) =>
                  val (var_list, ty) = instantiate_ty_ann (level + 1, ty_ann)
                  var_list_ref.a = var_list ::: var_list_ref.a
                  ty
              }
            fn_env_ref.a = fn_env_ref.a + (param_name -> param_ty)
            param_ty
          }

        val return_ty =
          infer(fn_env_ref.a, level + 1, maybe_expected_return_ty, body_generalized, body_expr)
        
        if (!var_list_ref.a.forall(is_monomorphic))
          error ("polymorphic parameter inferred: "
            + var_list_ref.a.map(string_of_ty).mkString(", "))
        
        maybe_generalize(generalized, level, TArrow(param_ty_list, return_ty))
    case Let(var_name, value_expr, body_expr) =>
        val var_ty = infer(env, level + 1, None, Generalized, value_expr)
        infer (env + (var_name -> var_ty), level, maybe_expected_ty, generalized, body_expr)
    case Call(fn_expr, arg_list) =>
        val fn_ty = instantiate(level + 1, infer(env, level + 1, None, Instantiated, fn_expr))
        val (param_ty_list, return_ty) = match_fun_ty(arg_list.length, fn_ty)
        val instantiated_return_ty = instantiate(level + 1, return_ty)
        (maybe_expected_ty, instantiated_return_ty) match {
          case (None, _) =>
          case (_, TVar(Ref(Unbound(_,_)))) =>
          case (Some(expected_ty), _) =>
            unify(instantiate(level + 1, expected_ty), instantiated_return_ty)
        }
        infer_args(env, level + 1, param_ty_list, arg_list)
        generalize_or_instantiate(generalized, level, instantiated_return_ty)
    case Ann(expr, ty_ann) =>
        val (_, ty) = instantiate_ty_ann(level, ty_ann)
        val expr_ty = infer(env, level, Some(ty), should_generalize(ty), expr)
        subsume(level, ty, expr_ty)
        ty
    }
  }

  def infer_args(env:Map[String,ty], level:level, param_ty_list:List[ty], arg_list:List[expr]) {
    val pair_list = param_ty_list.zip(arg_list)
    def get_ordering(ty:ty, arg:expr):Int = {
      // subsume annotated arguments first, type variables last
      if (is_annotated (arg)) 0
      else unlink(ty) match {
          case TVar(Ref(Unbound(_,_))) => 2
          case _ => 1
          }
    }
    val sorted_pair_list = pair_list.sortWith {
      case ((ty1, arg1), (ty2, arg2)) => get_ordering(ty1, arg1) > get_ordering(ty2, arg2)
    }
    
    sorted_pair_list.foreach {
      case (param_ty, arg_expr) =>
        val arg_ty = infer(env, level, Some(param_ty), should_generalize(param_ty), arg_expr)
        if (is_annotated(arg_expr))
          unify(param_ty, arg_ty)
        else
          subsume(level, param_ty, arg_ty)
    }
  }
  
}
