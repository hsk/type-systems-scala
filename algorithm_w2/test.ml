type varname = string

type exp = 
  | Var of varname                      (* variable                *)
  | Call of exp * exp                   (* application: e1 e2      *)
  | Fun of varname * exp                (* abstraction: fun x -> e *)
  | Let of varname * exp * exp          (* let x = e in e2         *)

(* 型から一般化された型を消して、generic_levelなら一般化されていることにする *)

type level = int
let generic_level = 100000000           (* as in OCaml typing/btype.ml *)
let marked_level = -1                   (* 再帰検査中を示すフラグ *)
type typ = 
  | TVar of tv ref
  | TArrow of typ * typ * levels
and tv =
  | Unbound of string * level
  | Link of typ

(* 関数内のレベル保存用 *)
and levels = {mutable level_old : level; mutable level_new : level}

let counter = ref 0
let reset_gensym () =
  counter := 0

(* 型変数生成 *)
let newvar level =
  let gensym () : string =
    let n = !counter in
    let () = incr counter in
    if n < 26 then String.make 1 (Char.chr (Char.code 'a' + n))
              else "t" ^ string_of_int n
  in
  TVar (ref (Unbound (gensym (), level)))

(* 関数生成 *)
let new_arrow : level -> typ -> typ -> typ = fun level ty1 ty2 ->
  TArrow(ty1,ty2,{level_new = level; level_old = level})

(* 型のリンクを置き換える *)
let rec repr (typ:typ): typ =
  match typ with
  | TVar ({contents = Link t} as tvr) -> 
      let t = repr t in
      tvr := Link t; t
  | t -> t

let to_be_level_adjusted = ref []
let reset_level_adjustment ()
    = to_be_level_adjusted := []

type env = (varname * typ) list

let rec unify : typ -> typ -> unit = fun t1 t2 ->
  (*
  受け取った型のレベルを変更する
  *)
  let update_level (level:level) (typ:typ): unit =
    match typ with
    (* 型変数の場合 *)
    | TVar ({contents = Unbound (n,level1)} as tvr) -> 
        (* 一般化変数は有り得ない *)
        assert (not (level1 = generic_level));
        (* 変数のレベルが深い場合、浅いほうへ持ち上げる *)
        if level < level1 then
          tvr := Unbound (n,level)
    (* 関数のレベルを再入せずに更新する *)
    | TArrow (_,_,ls) as ty -> 
        (* 一般化変数は有り得ない *)
        assert (not (ls.level_new = generic_level));
        (* レベルがマークレベルなら出現チェックエラー *)
        if ls.level_new = marked_level then failwith "occurs check";

        (* 変数のレベルが深い場合、浅いほうへ持ち上げる *)
        if level < ls.level_new then begin
          (* level_newとlevel_oldが同じ場合は、遅延リストに入っていない *)
          if ls.level_new = ls.level_old then
            (*遅延用リストに加える*)
            to_be_level_adjusted := ty :: !to_be_level_adjusted;
          (* レベルを更新するとlevel_newとlevel_oldは変わる *)
          ls.level_new <- level
        end
    | _ -> assert false
  in
  if t1 == t2 then ()                   (* t1 and t2 are physically the same *)
  else match (repr t1,repr t2) with
  | (TVar ({contents = Unbound (_,l1)} as tv1) as t1, (* unify two free vars *)
    (TVar ({contents = Unbound (_,l2)} as tv2) as t2)) ->
       (* bind the higher-level var *)
       if l1 > l2 then tv1 := Link t2 else tv2 := Link t1
  | (TVar ({contents = Unbound (_,l)} as tv), t')
  | (t',TVar ({contents = Unbound (_,l)} as tv)) -> 
      update_level l t';
      tv := Link t'
  | (TArrow (tyl1,tyl2,ll), TArrow (tyr1,tyr2,lr)) ->
      let unify_lev l ty1 ty2 =
        let ty1 = repr ty1 in
        update_level l ty1;
        unify ty1 ty2
      in
      if ll.level_new = marked_level || lr.level_new = marked_level then
        failwith "cycle: occurs check";
      let min_level = min ll.level_new lr.level_new in
      ll.level_new <- marked_level; lr.level_new <- marked_level;
      unify_lev min_level tyl1 tyr1;
      unify_lev min_level tyl2 tyr2;
      ll.level_new <- min_level; lr.level_new <- min_level
  | _ -> assert false

let generalize : level -> typ -> unit = fun level ty ->
  (* 型からレベル取得 *)
  let get_level : typ -> level = function
    | TVar {contents = Unbound (_,l)} -> l
    | TArrow (_,_,ls)                 -> ls.level_new
    | _ -> assert false
  in
  let force_delayed_adjustments level =
    let rec adjust_one acc = function
      | TArrow (_, _, ls) as ty when ls.level_old <= level ->
          ty::acc                         (* update later *)
      | TArrow (_, _, ls) when ls.level_old = ls.level_new ->
          acc                             (* already updated *)
      | TArrow (ty1, ty2, ls) ->
          let level = ls.level_new in
          ls.level_new <- marked_level;
          let acc = loop acc level ty1 in
          let acc = loop acc level ty2 in
          ls.level_new <- level;
          ls.level_old <- level; 
          acc
      | _ -> assert false
    and loop acc level ty = 
      match repr ty with
      | TVar ({contents = Unbound (name,l)} as tvr) when l > level ->
          tvr := Unbound (name,level); acc
      | TArrow (_,_,ls) when ls.level_new = marked_level ->
          failwith "occurs check"
      | TArrow (_,_,ls) as ty ->
          if ls.level_new > level then ls.level_new <- level;
          adjust_one acc ty
      | _ -> acc
    in
    to_be_level_adjusted :=
      List.fold_left adjust_one [] !to_be_level_adjusted
  in
  force_delayed_adjustments level;
  let rec loop ty =
    match repr ty with
    | TVar ({contents = Unbound (name,l)} as tvr)
      when l > level -> tvr := Unbound (name,generic_level)
    | TArrow (ty1,ty2,ls) when ls.level_new > level ->
      let ty1 = repr ty1 and ty2 = repr ty2 in
      loop ty1; loop ty2;
      let l = max (get_level ty1) (get_level ty2) in
      ls.level_old <- l; ls.level_new <- l (* set the exact level upper bound *)
    | _ -> ()
  in loop ty

let instantiate (level:level)(ty:typ) : typ = 
  let rec loop subst = function
    | TVar {contents = Unbound (name,l)} when
        l = generic_level -> 
        begin
          try (List.assoc name subst, subst)
          with Not_found ->
            let tv = newvar level in
            (tv, (name,tv)::subst)
        end
    | TVar {contents = Link ty} -> loop subst ty
    | TArrow (ty1,ty2,ls) when ls.level_new = generic_level ->
        let (ty1,subst) = loop subst ty1 in
        let (ty2,subst) = loop subst ty2 in
        (new_arrow level ty1 ty2, subst)
    | ty -> (ty, subst)
  in fst (loop [] ty)

let rec infer : level -> env -> exp -> typ = fun level env -> function
  | Var x     -> instantiate level (List.assoc x env)
  | Fun (x,e) -> 
      let ty_x = newvar level in
      let ty_e = infer level ((x,ty_x)::env) e in
      new_arrow level ty_x ty_e
  | Call (e1,e2) ->
      let ty_fun = infer level env e1 in
      let ty_arg = infer level env e2 in
      let ty_res = newvar level in
      unify ty_fun (new_arrow level ty_arg ty_res);
      ty_res
  | Let (x,e,e2) -> 
      let ty_e = infer (level + 1) env e in
      generalize level ty_e;
      infer level ((x,ty_e)::env) e2

(* 再帰の型チェック *)
let rec cycle_check : typ -> unit = function
  | TVar {contents = Unbound _} -> ()
  | TVar {contents = Link ty}   -> cycle_check ty
  | TArrow (_,_,ls) when ls.level_new = marked_level -> failwith "occurs check"
  | TArrow (t1,t2,ls) ->
      let level = ls.level_new in
      ls.level_new <- marked_level;
      cycle_check t1;
      cycle_check t2;
      ls.level_new <- level

let _ =
  let id = Fun ("x",Var"x") in
  let c1 = Fun ("x",Fun ("y",Call (Var"x",Var"y"))) in

  let top_type_check : exp -> typ = fun exp ->
    reset_gensym ();
    reset_level_adjustment ();
    let ty = infer 1 [] exp in
    cycle_check ty;
    ty
  in

  assert(
    TArrow (TVar {contents = Unbound ("a", 1)},
     TVar {contents = Unbound ("a", 1)}, {level_old = 1; level_new = 1})
       = 
      top_type_check id);

  assert(
    TArrow
     (TVar
       {contents =
         Link
          (TArrow (TVar {contents = Unbound ("b", 1)},
            TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1}))},
     TArrow (TVar {contents = Unbound ("b", 1)},
      TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1}),
     {level_old = 1; level_new = 1})
     =
       top_type_check c1);

  assert(
    TArrow
     (TArrow (TVar {contents = Unbound ("d", 1)},
       TVar {contents = Unbound ("e", 1)}, {level_old = 1; level_new = 1}),
     TArrow (TVar {contents = Unbound ("d", 1)},
      TVar {contents = Unbound ("e", 1)}, {level_old = 1; level_new = 1}),
     {level_old = 1; level_new = 1})
     =
     top_type_check (Let ("x",c1,Var"x")));

  assert(
    TArrow (TVar {contents = Unbound ("b", 1)},
     TVar {contents = Unbound ("b", 1)}, {level_old = 1; level_new = 1})
     =
     top_type_check (Let ("y",Fun ("z",Var"z"), Var"y")));

  assert(
    TArrow (TVar {contents = Unbound ("a", 1)},
     TArrow (TVar {contents = Unbound ("c", 1)},
      TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1}),
     {level_old = 1; level_new = 1})
     =
     top_type_check (Fun ("x", Let ("y",Fun ("z",Var"z"), Var"y"))));

  assert(
    TArrow (TVar {contents = Link (TVar {contents = Unbound ("c", 1)})},
     TVar {contents = Link (TVar {contents = Unbound ("c", 1)})},
     {level_old = 1; level_new = 1})
     =
     top_type_check (Fun ("x", Let ("y",Fun ("z",Var"z"), 
                                        Call (Var"y",Var"x")))));

  begin try 
    ignore (top_type_check (Fun ("x",Call (Var"x",Var"x"))));
    assert false
  with Failure e -> print_endline e
  end;

  begin try 
    ignore(top_type_check (Let ("x",Var"x",Var"x")));
    assert false
  with Not_found -> print_endline "unbound var"
  end;

  assert(
    TVar
     {contents =
       Link
        (TArrow (TVar {contents = Unbound ("c", 1)},
          TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1}))}
     =
     top_type_check (Let ("id",id, Call (Var"id",Var"id"))));

  assert(
    TArrow (TVar {contents = Unbound ("i", 1)},
     TVar {contents = Unbound ("i", 1)}, {level_old = 1; level_new = 1})
     =
     top_type_check (Let ("x",c1,
                        Let ("y",
                              Let ("z",Call(Var"x",id), Var "z"),
                             Var"y"))));

  (*
  fun x -> fun y -> let x = x y in fun x -> y x;;
  - : (('a -> 'b) -> 'c) -> ('a -> 'b) -> 'a -> 'b = <fun>
  *)
  assert(
    TArrow
     (TVar
       {contents =
         Link
          (TArrow
            (TVar
              {contents =
                Link
                 (TArrow (TVar {contents = Unbound ("d", 1)},
                   TVar {contents = Unbound ("e", 1)},
                   {level_old = 1; level_new = 1}))},
            TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1}))},
     TArrow
      (TVar
        {contents =
          Link
           (TArrow (TVar {contents = Unbound ("d", 1)},
             TVar {contents = Unbound ("e", 1)}, {level_old = 1; level_new = 1}))},
      TArrow (TVar {contents = Unbound ("d", 1)},
       TVar {contents = Unbound ("e", 1)}, {level_old = 1; level_new = 1}),
      {level_old = 1; level_new = 1}),
     {level_old = 1; level_new = 1})
     =
     top_type_check (Fun ("x", Fun ("y",Let ("x",Call (Var"x",Var"y"),
                                      Fun ("x",Call (Var"y",Var"x")))))));

  (* now sound generalization ! *)
  assert(
    TArrow (TVar {contents = Unbound ("a", 1)},
     TVar {contents = Unbound ("a", 1)}, {level_old = 1; level_new = 1})
     =
     top_type_check (Fun ("x", Let ("y",Var"x", Var"y"))));

  (* now sound generalization ! *)
  assert(
    TArrow (TVar {contents = Unbound ("a", 1)},
     TArrow (TVar {contents = Unbound ("c", 1)},
      TVar {contents = Unbound ("a", 1)}, {level_old = 1; level_new = 1}),
     {level_old = 1; level_new = 1})
     =
     top_type_check (Fun ("x", Let ("y",Fun ("z",Var"x"), Var"y"))));

  (* now sound generalization ! *)
  assert(
    TArrow
     (TVar
       {contents =
         Link
          (TArrow (TVar {contents = Unbound ("b", 1)},
            TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1}))},
     TArrow (TVar {contents = Unbound ("b", 1)},
      TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1}),
     {level_old = 1; level_new = 1})
     =
     top_type_check (Fun ("x", Let ("y",Fun ("z",Call (Var"x",Var"z")), Var"y"))));

  (* now sound generalization ! *)
  assert(
    TArrow
     (TVar
       {contents =
         Link
          (TArrow (TVar {contents = Unbound ("b", 1)},
            TVar
             {contents =
               Link
                (TArrow (TVar {contents = Unbound ("b", 1)},
                  TVar {contents = Unbound ("d", 1)},
                  {level_old = 1; level_new = 1}))},
            {level_old = 1; level_new = 1}))},
     TArrow (TVar {contents = Unbound ("b", 1)},
      TVar {contents = Unbound ("d", 1)}, {level_old = 1; level_new = 1}),
     {level_old = 1; level_new = 1})
     =
     top_type_check (Fun ("x", Fun ("y",Let ("x",Call (Var"x",Var"y"),
                                        Call (Var"x",Var"y"))))));

  (* now sound generalization ! *)
  begin try 
    ignore (top_type_check (Fun ("x",Let("y",Var"x", Call (Var"y",Var"y")))));
    assert false
  with Failure e -> print_endline e
  end;

  (* now sound generalization ! *)
  (* fun x -> let y = let z = x (fun x -> x) in z in y;;
     - : (('a -> 'a) -> 'b) -> 'b = <fun>
  *)
  assert(
    TArrow
     (TVar
       {contents =
         Link
          (TArrow
            (TArrow (TVar {contents = Unbound ("b", 1)},
              TVar {contents = Unbound ("b", 1)}, {level_old = 1; level_new = 1}),
            TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1}))},
     TVar {contents = Unbound ("c", 1)}, {level_old = 1; level_new = 1})
     =
     top_type_check (Fun ("x",
                        Let ("y",
                              Let ("z",Call(Var"x",id), Var "z"),
                              Var"y"))));
  print_endline "\nAll Done\n"

