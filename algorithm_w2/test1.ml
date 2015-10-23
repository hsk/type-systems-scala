type varname = string

type exp = 
  | Var of varname                      (* variable                *)
  | Call of exp * exp                    (* application: e1 e2      *)
  | Fun of varname * exp                (* abstraction: fun x -> e *)
  | Let of varname * exp * exp          (* let x = e in e2         *)

type qname = string
type level = int
type typ = 
  | TVar of tv ref               (* type (schematic) variable *)
  | QVar of qname                (* quantified type variable *)
  | TArrow of typ * typ
and tv = Unbound of string * level | Link of typ

let counter = ref 0
let reset_gensym : unit -> unit =
  fun () -> counter := 0

let gensym : unit -> string = fun () ->
  let n = !counter in
  let () = incr counter in
  if n < 26 then String.make 1 (Char.chr (Char.code 'a' + n))
            else "t" ^ string_of_int n

let newvar :level -> typ =
 fun level -> TVar (ref (Unbound (gensym (), level)))

let rec occurs : tv ref -> typ -> unit = fun tvr -> function
  | TVar tvr' when tvr == tvr' -> failwith "occurs check"
  | TVar ({contents = Unbound (name,l')} as tv) ->
      let min_level = 
        (match !tvr with Unbound (_,l) -> min l l' | _ -> l') in
      tv := Unbound (name,min_level)
  | TVar {contents = Link ty} -> occurs tvr ty
  | TArrow (t1,t2) -> occurs tvr t1; occurs tvr t2
  | _ -> ()

let rec unify : typ -> typ -> unit = fun t1 t2 ->
  if t1 == t2 then ()                   (* t1 and t2 are physically the same *)
  else match (t1,t2) with
  | (TVar ({contents = Unbound _} as tv),t')
  | (t',TVar ({contents = Unbound _} as tv)) -> occurs tv t'; tv := Link t'
  | (TVar {contents = Link t1},t2)
  | (t1,TVar {contents = Link t2}) -> unify t1 t2
  | (TArrow (tyl1,tyl2), TArrow (tyr1,tyr2)) ->
      unify tyl1 tyr1;
      unify tyl2 tyr2
  | _ -> assert false

type env = (varname * typ) list

let rec generalize level: typ -> typ = function
  | TVar {contents = Unbound (name,l)} 
      when l > level -> QVar name
  | TVar {contents = Link ty} -> generalize level ty
  | TArrow (ty1,ty2) -> TArrow (generalize level ty1, generalize level ty2)
  | ty -> ty

let instiate level : typ -> typ = 
  let rec loop subst = function
    | QVar name -> 
        begin
          try (List.assoc name subst, subst)
          with Not_found ->
            let tv = newvar level in
            (tv, (name,tv)::subst)
        end
    | TVar {contents = Link ty} -> loop subst ty
    | TArrow (ty1,ty2) -> 
        let (ty1,subst) = loop subst ty1 in
        let (ty2,subst) = loop subst ty2 in
        (TArrow (ty1,ty2), subst)
    | ty -> (ty, subst)
  in fun ty -> fst (loop [] ty)

let rec infer : level -> env -> exp -> typ = fun level env -> function
  | Var x     -> instiate level (List.assoc x env)
  | Fun (x,e) -> 
      let ty_x = newvar level in
      let ty_e = infer level ((x,ty_x)::env) e in
      TArrow(ty_x,ty_e)
  | Call (e1,e2) ->
      let ty_fun = infer level env e1 in
      let ty_arg = infer level env e2 in
      let ty_res = newvar level in
      unify ty_fun (TArrow (ty_arg,ty_res));
      ty_res
  | Let (x,e,e2) -> 
      let ty_e = infer (level + 1) env e in
      infer level ((x,generalize level ty_e)::env) e2

let _ =
  let id = Fun ("x",Var"x") in
  let c1 = Fun ("x",Fun ("y",Call (Var"x",Var"y"))) in

  let top_type_check : exp -> typ = fun exp ->
    reset_gensym ();
    infer 1 [] exp
  in
  assert(
    TArrow (TVar {contents = Unbound ("a", 1)},
     TVar {contents = Unbound ("a", 1)})
       = 
      top_type_check id);

  assert(
    TArrow
     (TVar
       {contents =
         Link
          (TArrow (TVar {contents = Unbound ("b", 1)},
            TVar {contents = Unbound ("c", 1)}))},
     TArrow (TVar {contents = Unbound ("b", 1)},
      TVar {contents = Unbound ("c", 1)}))
     =
       top_type_check c1);

  assert(
    TArrow
     (TArrow (TVar {contents = Unbound ("d", 1)},
       TVar {contents = Unbound ("e", 1)}),
     TArrow (TVar {contents = Unbound ("d", 1)},
      TVar {contents = Unbound ("e", 1)}))
     =
     top_type_check (Let ("x",c1,Var"x")));

  assert(
    TArrow (TVar {contents = Unbound ("b", 1)},
     TVar {contents = Unbound ("b", 1)})
     =
     top_type_check (Let ("y",Fun ("z",Var"z"), Var"y")));

  assert(
   TArrow (TVar {contents = Unbound ("a", 1)},
    TArrow (TVar {contents = Unbound ("c", 1)},
            TVar {contents = Unbound ("c", 1)}))
   =
   top_type_check (Fun ("x", Let ("y",Fun ("z",Var"z"), Var"y"))));

  assert(
   TArrow (TVar {contents = Unbound ("a", 1)},
    TVar
    {contents =
      Link (TVar {contents = Link (TVar {contents = Unbound ("a", 1)})})})
   =
   top_type_check (Fun ("x", Let ("y",Fun ("z",Var"z"), 
                                      Call (Var"y",Var"x")))));
  (try 
   ignore (top_type_check (Fun ("x",Call (Var"x",Var"x"))));
   assert false;
   with Failure e -> print_endline e
  );

  (try 
   ignore (top_type_check (Let ("x",Var"x",Var"x")));
   assert false;
   with Not_found -> print_endline "unbound var"
  );

  (* id can be `self-applied', on the surface of it *)
  assert(
   TVar
    {contents =
     Link
      (TVar
        {contents =
          Link
           (TArrow (TVar {contents = Unbound ("c", 1)},
             TVar {contents = Unbound ("c", 1)}))})}
   =
   top_type_check (Let ("id",id, Call (Var"id",Var"id"))));

  assert(
   TArrow (TVar {contents = Unbound ("i", 1)},
           TVar {contents = Unbound ("i", 1)})
   =
   top_type_check (Let ("x",c1,
                      Let ("y",
                            Let ("z",Call(Var"x",id), Var "z"),
                           Var"y"))));

  (*
  fun x -> fun y -> let x = x y in fun x -> y x);
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
                 TVar {contents = Unbound ("e", 1)}))},
          TVar {contents = Unbound ("c", 1)}))},
    TArrow
    (TVar
      {contents =
        Link
         (TArrow (TVar {contents = Unbound ("d", 1)},
           TVar {contents = Unbound ("e", 1)}))},
     TArrow (TVar {contents = Unbound ("d", 1)},
     TVar {contents = Unbound ("e", 1)})))
   =
   top_type_check (Fun ("x", Fun("y",Let ("x",Call (Var"x",Var"y"),
                                    Fun ("x",Call (Var"y",Var"x")))))));

  (* now sound generalization ! *)
  assert(
  TArrow (TVar {contents = Unbound ("a", 1)},
          TVar {contents = Unbound ("a", 1)})
   =
   top_type_check (Fun ("x", Let ("y",Var"x", Var"y"))));

  (* now sound generalization ! *)
  assert(
   TArrow (TVar {contents = Unbound ("a", 1)},
    TArrow (TVar {contents = Unbound ("c", 1)},
            TVar {contents = Unbound ("a", 1)}))
   =
   top_type_check (Fun ("x", Let ("y",Fun ("z",Var"x"), Var"y"))));

  (* now sound generalization ! *)
  assert(
   TArrow
    (TVar
     {contents =
       Link
        (TArrow (TVar {contents = Unbound ("b", 1)},
          TVar {contents = Unbound ("c", 1)}))},
    TArrow (TVar {contents = Unbound ("b", 1)},
     TVar {contents = Unbound ("c", 1)}))
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
                TVar {contents = Unbound ("d", 1)}))}))},
    TArrow (TVar {contents = Unbound ("b", 1)},
            TVar {contents = Unbound ("d", 1)}))
   =
   top_type_check (Fun ("x", Fun("y",Let ("x",Call (Var"x",Var"y"),
                                      Call (Var"x",Var"y"))))));

  (* now sound generalization ! *)
  (try 
   ignore (top_type_check (Fun ("x",Let("y",Var"x", Call (Var"y",Var"y")))));
   assert false;
   with Failure e -> print_endline e
  );

  (* now sound generalization ! *)
  (* fun x -> let y = let z = x (fun x -> x) in z in y);
     - : (('a -> 'a) -> 'b) -> 'b = <fun>
  *)
  assert(
   TArrow
   (TVar
     {contents =
       Link
        (TArrow
          (TArrow (TVar {contents = Unbound ("b", 1)},
            TVar {contents = Unbound ("b", 1)}),
          TVar {contents = Unbound ("c", 1)}))},
    TVar {contents = Unbound ("c", 1)})
   =
   top_type_check (Fun ("x",
                      Let ("y",
                            Let ("z",Call(Var"x",id), Var "z"),
                            Var"y"))));

  print_endline "\nAll Done\n"
