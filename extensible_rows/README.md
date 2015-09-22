extensible_rowsの拡張は以下のテストを通るようにします。

## diff test_parser.scala

    // records
    "{}" -> OK(RecordEmpty),
    "{ }" -> OK(RecordEmpty),
    "{" -> Fail,
    "a.x" -> OK(RecordSelect(Var("a"), "x")),
    "{m - a}" -> OK(RecordRestrict(Var("m"), "a")),
    "{m - a" -> Fail,
    "m - a" -> Fail,
    "{a = x}" -> OK(RecordExtend("a", Var("x"), RecordEmpty)),
    "{a = x" -> Fail,
    "{a=x, b = y}" -> OK(RecordExtend("a", Var("x"), RecordExtend("b", Var("y"), RecordEmpty))),
    "{b = y ,a=x}" -> OK(RecordExtend("b", Var("y"), RecordExtend("a", Var("x"), RecordEmpty))),
    "{a=x,h=w,d=y,b=q,g=z,c=t,e=s,f=r}" ->
      OK(RecordExtend("a", Var("x"), RecordExtend("h", Var("w"), RecordExtend("d", Var("y"),
        RecordExtend("b", Var("q"), RecordExtend("g", Var("z"), RecordExtend("c", Var("t"),
        RecordExtend("e", Var("s"), RecordExtend("f", Var("r"), RecordEmpty))))))))),
    "{a = x|m}" -> OK(RecordExtend("a", Var("x"), Var("m"))),
    "{a | m}" -> Fail,
    "{ a = x, b = y | m}" -> OK(RecordExtend("a", Var("x"), RecordExtend("b", Var("y"), Var("m")))),
    "{ a = x, b = y | {m - a} }" ->
      OK(RecordExtend("a", Var("x"), RecordExtend("b", Var("y"), RecordRestrict(Var("m"), "a")))),
    "{ b = y | m - a }" -> Fail,
    "let x = {a = f(x), b = y.b} in { a = fun z -> z | {x - a} }" ->
      OK(Let("x", RecordExtend("a", Call(Var("f"), List(Var("x"))), RecordExtend("b",
        RecordSelect(Var("y"), "b"), RecordEmpty)), RecordExtend("a", Fun(List("z"), Var("z")),
        RecordRestrict(Var("x"), "a"))))


## diff test_infer.scala

    // records
    "{}" -> OK("{}"),
    "{}.x" -> fail,
    "{a = one}" -> OK("{a : int}"),
    "{a = one, b = true}" -> OK("{a : int, b : bool}"),
    "{b = true, a = one}" -> OK("{b : bool, a : int}"),
    "{a = one, b = true}.a" -> OK("int"),
    "{a = one, b = true}.b" -> OK("bool"),
    "{a = one, b = true}.c" -> error("row does not contain label c"),
    "{f = fun x -> x}" -> OK("forall[a] {f : a -> a}"),
    "let r = {a = id, b = succ} in choose(r.a, r.b)" -> OK("int -> int"),
    "let r = {a = id, b = fun x -> x} in choose(r.a, r.b)" -> OK("forall[a] a -> a"),
    "choose({a = one}, {})" -> fail,
    "{ x = zero | { y = one | {} } }" -> OK("{x : int, y : int}"),
    "choose({ x = zero | { y = one | {} } }, {x = one, y = zero})" -> OK("{x : int, y : int}"),
    "{{} - x}" -> fail,
    "{{x = one, y = zero} - x}" -> OK("{y : int}"),
    "{ x = true | {x = one}}" -> OK("{x : bool, x : int}"),
    "let a = {} in {b = one | a}" -> OK("{b : int}"),
    "let a = {x = one} in {x = true | a}.x" -> OK("bool"),
    "let a = {x = one} in a.y" -> error("row does not contain label y"),
    "let a = {x = one} in {a - x}" -> OK("{}"),
    "let a = {x = one} in let b = {x = true | a} in {b - x}.x" -> OK("int"),
    "fun r -> {x = one | r}" -> OK("forall[r] {r} -> {x : int | r}"),
    "fun r -> r.x" -> OK("forall[r a] {x : a | r} -> a"),
    "let get_x = fun r -> r.x in get_x({y = one, x = zero})" -> OK("int"),
    "let get_x = fun r -> r.x in get_x({y = one, z = true})" -> error("row does not contain label x"),
    "fun r -> choose({x = zero | r}, {x = one | {}})" -> OK("{} -> {x : int}"),
    "fun r -> choose({x = zero | r}, {x = one})" -> OK("{} -> {x : int}"),
    "fun r -> choose({x = zero | r}, {x = one | r})" -> OK("forall[r] {r} -> {x : int | r}"),
    "fun r -> choose({x = zero | r}, {y = one | r})" -> error("recursive row types"),
    "let f = fun x -> x.t(one) in f({t = succ})" -> OK("int"),
    "let f = fun x -> x.t(one) in f({t = id})" -> OK("int"),
    "{x = one, x = true}" -> OK("{x : int, x : bool}"),
    "let f = fun r -> let y = r.y in choose(r, {x = one, x = true}) in f" ->
      error("row does not contain label y"),
    "fun r -> let y = choose(r.x, one) in let z = choose({r - x}.x, true) in r" ->
      OK("forall[a r] {x : int, x : bool | r} -> {x : int, x : bool | r}"),
    "fun r -> choose({x = zero | r}, {x = one, x = true})" -> OK("{x : bool} -> {x : int, x : bool}"),
    "fun r -> choose(r, {x = one, x = true})" -> OK("{x : int, x : bool} -> {x : int, x : bool}"),
    "fun r -> choose({x = zero | r}, {x = true | r})" -> error("cannot unify types int and bool")

## diff expr.ml

追加された構文木

	  case class RecordSelect(a:Expr, b: name) extends Expr          // selecting value of label: `r.a`
	  case class RecordExtend(a:name, b: Expr, c: Expr) extends Expr // extending a record: `{a = 1 | r}`
	  case class RecordRestrict(a: Expr, b: name) extends Expr       // deleting a label: `{r - a}`
	  case object RecordEmpty extends Expr                           // empty record: `{}`

型

	  case class TRecord(a:Row) extends Ty                  // record type: `{<...>}`
	  case object TRowEmpty extends Ty                      // empty row: `<>`
	  case class TRowExtend(a:name, b:Ty, c:Row) extends Ty // row extension: `<a : _ | ...>`
	  type Row = Ty // the kind of rows - empty row, row variable, or row extension

構文木の印字

      case RecordEmpty => "{}"
      case RecordSelect(record_expr, label) => f(true, record_expr) + "." + label
      case RecordRestrict(record_expr, label) => "{" + f(false, record_expr) + " - " + label + "}"
      case RecordExtend(label, expr, record_expr) =>
          def g(str:String, e:Expr):String = {
            e match {
            case RecordEmpty => str
            case RecordExtend(label, expr, record_expr) =>
              g(str + ", " + label + " = " + f(false, expr), record_expr)
            case other_expr => str + " | " + f(false, other_expr)
            }
          }
          "{" + g(label + " = " + f(false, expr), record_expr) + "}"


型の印字

        case TRecord(row_ty) => "{" + f(false, row_ty) + "}"
        case TRowEmpty => ""
        case TRowExtend(label, ty, row_ty) =>
          def g(str: String, t:Ty): String = {
            t match {
              case TRowEmpty => str
              case TRowExtend(label, ty, row_ty) =>
                g(str + ", " + label + " : " + f(false, ty), row_ty)
              case TVar(Ref(Link(ty))) => g(str, ty)
              case other_ty => str + " | " + f(false, other_ty)
            }
          }
          g(label + " : " + f(false, ty), row_ty)


## infer.ml


型推論

        case TRecord(row) => f(row)
        case TRowExtend(label, field_ty, row) => f(field_ty); f(row)
        case TRowEmpty =>

TODO:

> 		| TRecord row1, TRecord row2 -> unify row1 row2
> 		| TRowEmpty, TRowEmpty -> ()
> 		| TRowExtend(label1, field_ty1, rest_row1), (TRowExtend _ as row2) -> begin
> 				let rest_row1_tvar_ref_option = match rest_row1 with
> 					| TVar ({contents = Unbound _} as tvar_ref) -> Some tvar_ref
> 					| _ -> None
> 				in
> 				let rest_row2 = rewrite_row row2 label1 field_ty1 in
> 				begin match rest_row1_tvar_ref_option with
> 					| Some {contents = Link _} -> error "recursive row types"
> 					| _ -> ()
> 				end ;
> 				unify rest_row1 rest_row2
> 			end


> and rewrite_row row2 label1 field_ty1 = match row2 with
> 	| TRowEmpty -> error ("row does not contain label " ^ label1)
> 	| TRowExtend(label2, field_ty2, rest_row2) when label2 = label1 ->
> 			unify field_ty1 field_ty2 ;
> 			rest_row2
> 	| TRowExtend(label2, field_ty2, rest_row2) ->
> 			TRowExtend(label2, field_ty2, rewrite_row rest_row2 label1 field_ty1)
> 	| TVar {contents = Link row2} -> rewrite_row row2 label1 field_ty1
> 	| TVar ({contents = Unbound(id, level)} as tvar) ->
> 			let rest_row2 = new_var level in
> 			let ty2 = TRowExtend(label1, field_ty1, rest_row2) in
> 			tvar := Link ty2 ;
> 			rest_row2
> 	| _ -> error "row type expected"
> 


< 	| TVar {contents = Generic _} | TVar {contents = Unbound _} | TConst _ as ty -> ty
---
> 	| TRecord row -> TRecord (generalize level row)
> 	| TRowExtend(label, field_ty, row) ->
> 			TRowExtend(label, generalize level field_ty, generalize level row)
> 	| TVar {contents = Generic _} | TVar {contents = Unbound _} | TConst _ | TRowEmpty as ty -> ty


> 		| TRecord row -> TRecord (f row)
> 		| TRowEmpty -> ty
> 		| TRowExtend(label, field_ty, row) ->
> 				TRowExtend(label, f field_ty, f row)


> 	| RecordEmpty -> TRecord TRowEmpty
> 	| RecordSelect(record_expr, label) ->
> 			(* inlined code for Call of function with type "forall[a r] {label : a | r} -> a" *)
> 			let rest_row_ty = new_var level in
> 			let field_ty = new_var level in
> 			let param_ty = TRecord (TRowExtend(label, field_ty, rest_row_ty)) in
> 			let return_ty = field_ty in
> 			unify param_ty (infer env level record_expr) ;
> 			return_ty
> 	| RecordRestrict(record_expr, label) ->
> 			(* inlined code for Call of function with type "forall[a r] {label : a | r} -> {r}" *)
> 			let rest_row_ty = new_var level in
> 			let field_ty = new_var level in
> 			let param_ty = TRecord (TRowExtend(label, field_ty, rest_row_ty)) in
> 			let return_ty = TRecord rest_row_ty in
> 			unify param_ty (infer env level record_expr) ;
> 			return_ty
> 	| RecordExtend(label, expr, record_expr) ->
> 			(* inlined code for Call of function with type "forall[a r] (a, {r}) -> {label : a | r}" *)
> 			let rest_row_ty = new_var level in
> 			let field_ty = new_var level in
> 			let param1_ty = field_ty in
> 			let param2_ty = TRecord rest_row_ty in
> 			let return_ty = TRecord (TRowExtend(label, field_ty, rest_row_ty)) in
> 			unify param1_ty (infer env level expr) ;
> 			unify param2_ty (infer env level record_expr) ;
> 			return_ty

## parser.mly


> let expr_record_extend label_expr_list record =
> 	List.fold_left
> 		(fun record (label, expr) -> RecordExtend(label, expr, record))
> 		record label_expr_list
> 
> let ty_row_extend label_ty_list row =
> 	List.fold_left
> 		(fun row (label, ty) -> TRowExtend(label, ty, row))
> 		row label_ty_list
> 


< 		| TApp(ty, ty_arg_list) ->
< 				TApp(f ty, List.map f ty_arg_list)
< 		| TArrow(param_ty_list, return_ty) ->
< 				TArrow(List.map f param_ty_list, f return_ty)
---
> 		| TApp(ty, ty_arg_list) -> TApp(f ty, List.map f ty_arg_list)
> 		| TArrow(param_ty_list, return_ty) -> TArrow(List.map f param_ty_list, f return_ty)
> 		| TRecord row -> TRecord (f row)
> 		| TRowEmpty -> ty
> 		| TRowExtend(label, ty, row) -> TRowExtend(label, f ty, f row)



> 	| LBRACE RBRACE                                     { RecordEmpty }
> 	| LBRACE record_label_expr_list PIPE expr RBRACE    { expr_record_extend $2 $4 }
> 	| LBRACE record_label_expr_list RBRACE              { expr_record_extend $2 RecordEmpty }
> 	| LBRACE expr MINUS IDENT RBRACE                    { RecordRestrict($2, $4) }
> 	| simple_expr DOT IDENT                             { RecordSelect($1, $3) }


> record_label_expr_list:
> 	| IDENT EQUALS expr                               { [($1, $3)] }
> 	| record_label_expr_list COMMA IDENT EQUALS expr  { ($3, $5) :: $1 }
> 


> 	| LBRACE RBRACE                                 { TRecord TRowEmpty }
> 	| LBRACE IDENT RBRACE                           { TRecord (TConst $2) }
> 	| LBRACE ty_row PIPE ty RBRACE                  { TRecord (ty_row_extend $2 $4) }
> 	| LBRACE ty_row RBRACE	                        { TRecord (ty_row_extend $2 TRowEmpty) }


> ty_row:
> 	| IDENT COLON ty                    { [($1, $3)] }
> 	| ty_row COMMA IDENT COLON ty       { ($3, $5) :: $1 }

