# Refined types: a better type system for more secure software

# Refined types：超安全ソフトウェア用最適型システム

|English|日本語|
| --- | --- |
|Refined|洗練された|
|experiment|実験|
|combines|兼ね備え|
|dependent|依存|
|Although|であるが|
|i.e.|すなわち|
|contracts|契約|
|prove|証明する|
|absence|ない状態|

> <sup><sub>
This is another type systems experiment that combines Hindley–Milner type inference with static type-checking of a limited version of dependent types called *refined types*.
Although the type-checker only allows refined types on function parameters and return types (i.e. *function contracts*), it can prove the absence of some of the most common software bugs.

これは、*refined types* と呼ばれる依存型の限定バージョンの静的型チェックでヒンドリーミルナーの型推論を組み合わせた別の型システム実験です。
型チェッカが唯一の関数のパラメータおよび戻り値の型（関数契約）の洗練化タイプを可能にするが、それは最も一般的なソフトウェアのバグのいくつかが存在しないことを証明することができます。

|English|日本語|
| --- | --- |
|consider|考慮する|
|division|除算|
|denominator|分母|
|Thus|したがって、|
|as would|同じように|
|deduce|演繹する|
|potentially|潜在的に|
|non-deterministic|非決定的|
|during|間に|
|compilation|コンパイル|

> <sup><sub>
For a simple example, let's consider integer division: we know that the denominator cannot be zero.
Thus, if we define division as `/ : (int, i : int if i != 0) → int`, the refined type-checker can tell us *during compilation* that `1/0` will result in an error, as would `1/(2 * 3 - 6)` and `1/(4 % 2)`.
The system can also deduce that the program `10 / (random1toN(10) - 5)` is potentially unsafe, where `random1toN` is a non-deterministic function whose type is `(N : int if N ≥ 1) → (i : int if 1 ≤ i and i ≤ N)`.

簡単な例について、整数除算を考えてみましょう：分母がゼロにならないことを私たちは知っています。
従って、我々は`/ : (int, i : int if i != 0) → int`と割り算を定義した場合、Refined typesチェッカは `1/0` あるいは `1/(2 * 3 - 6)` や `1/(4 % 2)` がエラーになることを、コンパイル時に私たちに伝えることができます。
このシステムは、プログラム`10 / (random1toN(10) - 5)`が安全でない可能性があることを推論することができます(ここで`random1toN`は型`(N : int if N ≥ 1) → (i : int if 1 ≤ i and i ≤ N)`である非決定的関数)。

|English|日本語|
| --- | --- |
|verify|確認します|
|accessed|アクセス|
|out of bounds|範囲外|
|appropriate|適切な|
|contracts|契約|
|prevented|防止|

> <sup><sub>
Refined type checking can also be used to verify that arrays are not accessed out of bounds, and using appropriate contracts on functions `alloc` and `memcpy`, software bugs such as [Heartbleed][heartbleed] could be prevented.

Refined type checking も配列が境界外へのアクセス、および関数の`alloc`と`memcpy`を、[Heartbleed][heartbleed]などのソフトウェアのバグを防ぐことができた上で適切な契約を使用していないことを確認するために使用することができます。

```typescript
alloc : (i : int) -> (a : array[byte] if length(a) == i)
memcpy : (dst : array[byte], src : array[byte],
          num : int if num <= length(dst) and num <= length(src)) -> unit

function heartbleed_bug(payload : array[byte], payload_length : int) {
  let response = alloc(payload_length)
  memcpy(response, payload, payload_length)    // ERROR!
  return response
}

function heartbleed_fix(payload : array[byte],
                        payload_length : int if length(payload) == payload_length) {
  let response = alloc(payload_length)
  memcpy(response, payload, payload_length)
  return response
}
```

|English|日本語|
| --- | --- |
|actually|実際に|
|straightforward|簡単な|
|turned out|判明|
|simpler|単純な|
|Essentially|基本的に|
|converted|変換されました|
|series|シリーズ|
|mathematical|数学の|
|formulas|式|
|validity|妥当性|
|assessed|評価|
|prover|証明器|
|handled|取り扱い|
|explained|説明|
|below|以下|

> <sup><sub>
The implementation of a refined type-checker is actually very straightforward and turned out to be
much simpler than I expected. Essentially, program expressions and contracts on function parameters
and return types are converted into a series of mathematical formulas and logical statements, the
validity of which is then assessed using an external automated theorem prover [Z3][z3]. The details
of the implementation, including the tricks that allow functions to be handled as first-class
values, are explained below.

Refined typesチェッカの実装は、実際には非常に簡単であり、私の予想よりはるかに簡単であることが判明しました。基本的に、プログラム式や関数のパラメータと戻り値の型の契約は、外部自動定理証明[Z3][z3]を用いて評価される有効性そのうちの数式や論理ステートメントの列に変換されます。
関数はファーストクラスの値として扱うことを可能にするトリックを含む実装の詳細は、以下に説明します。

|English|日本語|
| --- | --- |
|should|すべきです|
|familiar|おなじみの|
|its|それの|

> <sup><sub>
*Note about syntax:* These examples use a syntax similar to JavaScript or TypeScript that
should be familiar to most programmers, which is different from the [ML][ml-language]-like syntax that the
type-checker and its test cases use.

*構文に関する注意:* これらの例は、JavaScriptやタイプスクリプトに似た構文を使用していて、それは、ほとんどのプログラマに精通している必要があり、これは型チェッカとそのテストケースを使用して、MLのような構文とは異なります。


## Overview

## 概要

|English|日本語|
| --- | --- |
|i.e.|すなわち|
|often|多くの場合|
|presented|提示|
|holy grail|至高の目標|
|yet|まだ|
|despite|かかわらず、|
|intensive|集中的な|
|remain|残ります|
|impractical|非現実的な|
|predicates|述語|
|notation|表記法|
|commonly|一般的に|
|academic|アカデミック|
|literature|文献|

> <sup><sub>
*Dependent types*, i.e. types that depend on values, are often presented as the holy grail of secure
static type systems, yet despite intensive research they remain complex and impractical and are only
used in research languages and mathematical proof assistants. *Refined types* or *contracts* are a
restricted form of dependent types that combine base datatypes with logical predicates; for example,
the type of natural numbers could be written `x : int if x ≥ 0` (the notation most commonly used in
academic literature is `{ν : int | ν ≥ 0}`).

*依存型*, すなわち値に依存する型は、多くの場合、安全な静的型システムの究極の目標として提示されており、集中的な研究にもかかわらず、彼らは、複雑で非現実的なままであり、研究言語と数学的証明アシスタントでのみ使用されています。
*Refined types* または契約は、論理的な述語で基本データ型を組み合わせた依存型の制限された形です; 例えば、自然数のタイプは、`x : int if x ≥ 0` と書くことができます。(最も一般的に学術文献で使用される表記は `{ν : int | ν ≥ 0}` です)。

|English|日本語|
| --- | --- |
|experimentation|実験|
|past|過去|
|decade|10年|
|Hybrid|ハイブリッド|
|contracts|契約|
|deferring|延期|
|Limited|限られました|
|reduce|減らします|
|amount|量|
|prove|証明します|
|safety|安全性|
|Liquid|液状の|
|also|また|
|experimental|実験的|
|superseded|置き換え|
|since|以来、|

> <sup><sub>
Refined types have been a topic of a lot of research and experimentation in the past decade. *Hybrid
type checking* [1] combines static and dynamic type-checking by verifying the contracts statically
when possible and deferring the checks until runtime when necessary (implemented in programming
language [Sage][sage] [2]). Limited automatic inference of function contracts was developed which
can reduce the amount of type annotations necessary to prove software safety (e.g. Liquid Types [3]
and [4]). Refined types have also been used in some experimental programming languages and verifying
compilers, such as the [VCC][vcc], a verifier for concurrent C, [F7][f7], which implements refined
types for F# (since superseded by [F*][f-star]), and the [Whiley][whiley] programming language.

Refined types は、過去十年間の間、多くの研究と実験の話題となっています。
ハイブリッド型検査 [1] は、静的に契約を確認し、可能な場合、必要に応じて実行時までチェックを延期することにより、静的および動的な型チェックを兼ね備えています(プログラミング言語[Sage][sage] [2]で実装されています）。
関数契約の制限された自動推論タイプ、ソフトウェアの安全性を証明するために必要な注釈の量を低減することができる開発された（例えば、液体タイプ[3]、[4]）。
洗練されたタイプはまた、同時Cについて[VCC][vcc]、検証として、いくつかの実験的なプログラミング言語や検証コンパイラで使用されている、 F取って代わらので、（F＃のための洗練されたタイプを実装し、[F7][f7] [F*][F-star]）、および[Whiley][whiley]プログラミング言語。

|English|日本語|
| --- | --- |
|primarily|主に|
|nevertheless|それにもかかわらず|
|variety|多様|
|strips|ストリップ|
|provers|証明系|
|SMT solvers|SMTソルバ|
|complicated|複雑な|
|contracts|契約|
|instead|代わりに|
|formulas|式|

> <sup><sub>
This experiment, inspired primarily by Sage and Liquid Types, is an implementation of refined type-checking for a simple functional language.
Refined types are only allowed on function parameters and return types; nevertheless, a variety of static program properties can be verified.
The type-checker first strips all refined type annotations and uses Hindley–Milner type inference to infer base types of functions and variables.
Then it translates the program into [SMT-LIB][smtlib], a language understood by automated theorem provers called *SMT solvers*.
SMT solvers understand how integers and booleans work, so simple expressions such as `1 + a` can be translated directly.
Translation of functions is more complicated, as SMT solvers use first-order logic and cannot handle functions as first-class values, so the contracts on their parameters and return types are translated instead.
The resulting SMT-LIB formulas are run through a SMT solver (this implementation uses [Z3][z3]) to verify that none of the translated contracts are broken.

セージとリキッドタイプによって主にインスピレーションを得たこの実験は、単純な関数型言語のための洗練された型チェックの実装です。
洗練されたタイプは、唯一の関数パラメータと戻り値の型で許可されています;それにもかかわらず、静的なプログラムのプロパティの様々な検証することができます。
型チェッカは、最初にすべての洗練された型の注釈を取り除き、関数や変数のベース型を推論するヒンドリー - ミルナーの型推論を使用しています。
次に、[SMT-LIB][smtlib]自動定理証明によって理解される言語と呼ばれる*SMTソルバ*にプログラムを変換します。
SMTソルバは整数とブールの仕事なので、単純な式はのような`1 + a`直接翻訳することができる方法を理解します。
SMTソルバーが一階論理を使用し、それらのパラメータにファーストクラスの値として機能するので、契約を扱うことができないとタイプが代わりに翻訳されて返すような機能の翻訳は、より複雑です。
その結果、SMT-LIB式はSMTソルバーを介して実行されます(この実装の用途は[Z3][z3])翻訳契約のいずれが壊れていないことを確認します。

> <sup><sub>
This design allows the refined type-checker to handle a variety of programming constructs, such as multiple variable definitions, nested function calls, and if statements. It can also track abstract properties such as array length or integer ranges, and handle function subtyping.
The following examples demonstrate these features:

このデザインは、洗練された型チェッカは、複数の変数定義、ネストされた関数呼び出し、およびif文プログラミング構造の多様性を処理することができます。
それはまた、配列の長さまたは整数範囲と抽象プロパティを追跡し、機能のサブタイプを処理することができます。
次の例では、これらの機能を示します:

```typescript
function cannot_get_first(arr : array[int]) {
  return get(arr, 0)    // ERROR!
}

function maybe_get_first(arr : array[int]) {
  if not is_empty(arr) {
    return get(arr, 0)
  } else {
    return -1
  }
}

function get_2dimensional(n : int if n >= 0, m : int if m >= 0,
                          i : int if 0 <= i and i < m, j : int if 0 <= j and j < n,
                          arr : array[int] if length(arr) == m * n) {
  return get(arr, i * n + j)
}

function max_typo(x, y) : (z : int if z >= x and z >= y) {
  if x <= y {     // Oops, should be `x >= y`!
    return x      // ERROR: `z` can be less than `y`
  } else {
    return y
  }
}

function test(x : int if abs(x) <= 10) {
  let z =
    if max(square(x), 25) == 25 {
      3 * x + 7 * random1toN(10)
    } else if x == 11 {     // cannot happen
      0
    } else {
      x
    }
  return 100 / z
}

/* function subtyping */
min : (i : int if i > 0, j : int if j < 0) -> (k : int if k < 0)
make_const(1) : int -> (a : int if a == 1)
```

|English|日本語|
| --- | --- |
|particularly|特に|
|arithmetic|算術|
|incomplete|不完全|
|undecidable|決定不能な|
|Although|であるが|
|prove|証明する|
|within|内部で|
|Instead|代わりに、|
|Even though|たとえ、にもかかわらず|
|decidable|決定可能な|
|certain|一定|
|disprove|反証する|
|equalities|等式|

> <sup><sub>
The `get_2dimensional` function is particularly interesting; it uses [non-linear integer arithmetic][robinson-arithmetic], which is incomplete and undecidable.
Although Z3 can prove simple non-linear statements about integers, such as `x² ≥ 0`, it cannot prove that the array is accessed within bound in the function `get_2dimensional`.
Instead, it has to convert the formula to real arithmetic and use the NLSat solver [5].
Even though non-linear real arithmetic is complete and decidable, this approach only works for certain kinds of problems; for example, it cannot disprove equalities that have real solutions but no integer ones, such as `x³ + y³ == z³` where `x`, `y` and `z` are positive.

`get_2dimensional`関数は特に興味深いです; これは[non-linear integer arithmetic][robinson-arithmetic]を使っていて、それは不完全で決定不能です。
Z3は、 `x² ≥ 0` のような整数についての簡単な非線形文を、証明することができますが、配列が関数 `get_2dimensional`中の結合内でアクセスされていることを証明することはできません。
その代わりに、それは本当の算術式を変換しNLSatソルバ[5]を使用する必要があります。
実際の計算が完了し、決定可能であっても非線形かかわらず、このアプローチは、ある種の問題のために動作します。
例えば、このようなそ正である`x`, `y` ,`z`について`x³ + y³ == z³`のような真の解決策が、なし整数のものを持っている等式を反証することはできません。

> <sup><sub>
> Implementation

## 実装

> <sup><sub>
>> Type inference

### 型推論

|English|日本語|
| --- | --- |
|slightly|少し|
|modified|変更された|
|perform|実行する|
|instead|代わりに|
|merely|単に、だけで|
|later|後で|
|predicate|述語|
|as well|同様に|
|prevent|防ぐ|
|unexpectedly|予想に反し|
|propagating|伝播|

> <sup><sub>
After lexing and parsing, a slightly modified [**algorithm-w**][algorithm-w] is used to perform
standard Hindley-Milner unification-based type inference on the AST. The main difference is that
instead of merely inferring the type of the input expression, the algorithm also transforms the AST
into a typed expression tree that will be used later by the refined type-checker. The predicate
expressions in refined function types have their types inferred as well and unified with `bool`. To
prevent unification from unexpectedly propagating refined types, predicates are stripped from
function types before calling `unify` and before adding the types to the typing context.

字句解析と構文解析した後、わずかに変更された[**algorithm-w**][algorithm-w]は、ASTに標準的なヒンドリー - ミルナー単一化ベースの型推論を実行するために使用されます。
主な違いではなく、単に入力式の型を推論する、アルゴリズムはまた、洗練された型チェッカによって、後に使用される型指定された式ツリーにASTを変換することです。
洗練された関数型で述語式は、その型も同様に推測し、ブール値で統一してい。
予想外に洗練されたタイプを伝播するから統一を防ぐために、述語は統一呼び出す前に、と入力コンテキストにタイプを追加する前に、関数の型から削除されます。

For example, the function cast
例えば、cast関数

```typescript
f : (x : int if x + 1 >= 0) -> int
```

|English|日本語|
| --- | --- |
|algorithm|アルゴリズム|
|roughly|おおよそ|
|representation|表現|
|denotes|表します|

is translated by the type inference algorithm roughly into the following representation, where
`{e; τ}` denotes a typed tree node with expression `e` and type `τ`:

おおよそ `{e; τ}` は、式 `e` と型 `τ` を持つ型付きツリーノードを表し、次の表現、に型推論アルゴリズムで翻訳されています：


```typescript
{
	{f; int -> int} : (x : int if {{{x; int} + {1; int}; int} >= {0; int}; bool}) -> int;
	int -> int
}
```

> <sup><sub>
>> Refined type-checking

### Refined 型検査

|English|日本語|
| --- | --- |
|proving|証明|
|reasoned|筋の通った、推論|
|precise|正確な|
|interpretations|解釈|
|literally|文字通り|
|specific|特定|
|certain|一定|
|more-or-less|多かれ少なかれ|
|assertions|の表明|

> <sup><sub>
The goal of refined type-checking is *proving* that none of the function contracts can be broken at runtime.
To do this, expressions of the source program must be translated into SMT-LIB formulas, so they can be reasoned about in proofs by the SMT solver.
Some expressions, such as integer constants and applications of built-in operators (e.g. `+`, `%`, `>=`, `==` and `or`), have precise values or interpretations in SMT theories and can be translated literally.
Others, such as function parameters and the return value of a `random1toN(10)` call, don't have specific values and we can only make certain more-or-less precise assertions about them.

Refined型チェックの目的は、*証明は*関数契約のいずれもが実行時に破損しないことが可能です。
これを行うには、ソースプログラムの表現は、SMT-LIB式に翻訳されなければならないので、SMTソルバーによって証明中について推論することができます。
このような整数定数と組み込み演算子のアプリケーションなどの一部の表現 (例えば、 `+`、`%`、`>=`、`==`と`or`)、 SMT理論で正確な値または解釈を持ってすることができます直訳。
このような関数のパラメータと `random1toN(10)`呼び出しの戻り値として、他のものは、特定の値を持っていないと私たちはそれらについて一定の多かれ少なかれ正確なアサーションを行うことができます。

|English|日本語|
| --- | --- |
|contract|契約|
|satisfied|満足、満たされた|
|examine|調べる|
|during|間に|

> <sup><sub>
We can use the SMT-LIB representation of an expression to check if a contract is satisfied.
For a simple example, let's examine the SMT-LIB script generated during refined type-checking of the function `test`:

我々は契約が成立しているかどうかを確認するために、式のSMT-LIB表現を使用することができます。簡単な例では、のは、関数 `test`のrefined 型チェックの際に生成されたSMT-LIBスクリプトを調べてみましょう：

```typescript
function test(x : int if x > 3) : (z : int if z > 0) {
	return x - 2
}
```

|English|日本語|
| --- | --- |
|declare|宣言する|

> <sup><sub>
We first declare a new SMT-LIB variable for the parameter `x`.
Its value is unknown and the most we can say about it is that `x > 3`.

私たちは、最初のパラメータ `x` の新しい SMT-LIB 変数を宣言します。
その値は不明であり、我々はそれについて言うことができるほとんどはそれを `x > 3` です。

```lisp
(declare-const x Int)                   ; declare `x : int`
(assert (>= (- x 1) 3))                 ; equivalent to `x > 3`
(push)                                  ; enter new stack frame
(assert (not (>= (- (- x 2) 1) 0)))     ; equivalent to `not (z > 0)` where `z == x - 2`
(check-sat)                             ; check satisfiability
(pop)                                   ; exit last stack frame
```

|English|日本語|
| --- | --- |
|satisfied|満足|
|validity|妥当性|
|implication|意味合い|
|premises|の前提|
|conclusion|結論|
|satisfiable|満足できる、充足可能な|
|Fortunately|幸いなことに|
|negating|ネゲート|
|whether|かどうか|
|negation|否定|
|satisfiable|満足できます|
|produces|生成|
|counterexample|反例|
|negated|否定|
|conclude|結論を出す|
|neither|どちらもありません|
|negated|否定|
|incorrectly|間違って|
|strict|厳格な|
|inequalities|不等式|
|instead|代わりに|

> <sup><sub>
To prove that a contract is satisfied, we need to prove the *validity* of the logical implication where all previous formulas and assertions are premises and the contract is the conclusion.
In the above example, the required implication is `x > 3 ⇒ x - 2 > 0`.
However, SMT solvers can only prove that a formula is *satisfiable* (there exists an assignment of values to the variables that makes the formula true), not that it is *valid* (it is true for every assignment of values).
Fortunately, we can determine if the implication is valid by negating the condition of the contract and checking whether the negation of the implication is satisfiable.
If the SMT solver produces a model showing that it is, indeed, satisfiable, we have a counterexample of values that break the contract.
If the SMT solver proves that the negated implication is not satisfiable, we conclude that the implication itself is valid, and that the contract cannot be broken.
If the solver can neither show that the negated implication is satisfiable nor prove that it is not, its satisfiability is checked again in the theory of non-linear real arithmetic by the NLSat solver.
(Z3 incorrectly translates strict inequalities when translating between the theories of integer and real arithmetic, which is why `>=` and `<=` are used instead of `>` and `<`.) 

契約が成立していることを証明するために、我々は以前のすべての数式やアサーションが前提であり、契約が締結され、論理的含意の*有効*を証明する必要があります。
上記の例では、必要な含意は `x > 3 ⇒ x - 2 > 0` です。
しかし、SMTソルバのみ式が*充足*であることを証明することができます（真の式になり、変数への値の割り当てが存在する）、それが*有効*であることではない（それは、値のすべての割り当てのために真です）。
含意が契約の条件を否定し、含意の否定が充足可能かどうかをチェックすることで有効であれば幸いなことに、我々は決定することができます。
SMTソルバは、それは、確かに、充足であることを示すモデルを作成する場合、我々は契約を破る値の反例を持っています。
SMTソルバは否定的含意が充足でないことを証明した場合、我々は意味合い自体が有効であること、及び契約は分割できないと結論付けています。
ソルバーはどちらも否定的含意が充足であることを示していることも、そうでないことを証明できる場合は、その充足がNLSatソルバーにより非線形実際の算術の理論に再びチェックされます。（整数と実数算術の理論との間で変換するときZ3が間違って `>=`と `<=` ではなく`>`と`<`の使用されている理由である、厳格な不平等を変換します。）

|English|日本語|
| --- | --- |
|trivially|自明に、トリビアルに|
|representation|表現|
|premises|の前提|
|negation|否定|
|premises|の前提|
|non-trivial|非自明|
|contract|契約|
|corresponding|対応する|
|refer|参照する|
|earlier|以前|
|so that|そのため、|
|contract|契約|
|correctly|正しく|

> <sup><sub>
Some expressions, such as integers, booleans and variables that do not have function types, can be trivially translated into SMT-LIB representation, but the translation of other kinds of expressions can be tricky.
When translating an `if` expression, the boolean condition has to be added to the premises when checking contracts in the `then` branch, while its negation has to be added to the premises when checking the `else` branch.
Another non-trivial case is checking function calls, where each argument expression is translated and the contract on the corresponding parameter must be checked.
As contracts on function parameters can refer to earlier parameters, the representations of argument expressions corresponding to named parameters are added to the function's *local environment*.
In the example above, the local environment when checking the refined return type is `{z ↦ "(- x 2)"}`, so that the variable `z` in the contract expression is translated correctly.

このような関数型を持っていない整数、ブール値や変数、などのいくつかの表現は、自明SMT-LIB 表現に変換することができますが、式の他の種類の翻訳は注意が必要です。
`if` 式を変換する場合、ブール条件は、その否定は `else` ブランチをチェックする際に施設に追加する必要がありながら、`then` ブランチの契約をチェックするときに施設に追加する必要があります。
別の非自明な場合は、各引数式が変換され、対応するパラメータの契約がチェックされなければならない関数呼び出しを、チェックしています。
関数のパラメータの契約は、以前のパラメータを参照することができますように、名前付きパラメータに対応する引数式の表現は、関数のローカル環境に追加されます。
契約式の変数 `z` が正しく変換されるように、上記の例では、洗練された戻り値の型をチェックし、ローカル環境は、`{z ↦ "(- x 2)"}` です。

|English|日本語|
| --- | --- |
|specifically|特に|
|uninterpreted|解釈されていません|
|whose|その|
|tracked|追跡|
|reasoned|筋の通った、推論|
|constrained|制約|

> <sup><sub>
The results of some function calls are represented directly, specifically the results of calls of built-in operators, which have standard interpretations in SMT theories, and *uninterpreted functions* such as `length`, which are used to represent abstract properties and whose values can be tracked and reasoned about by SMT solvers.
The results of other function calls are represented by fresh SMT variables, which are constrained by the contract on the functions return type.
For example, the result of the function application `x + 6` is represented by `"(+ x 6)"`, while the result of the call `random1toN(10)` is translated as

いくつかの関数呼び出しの結果は、直接SMT理論における標準的な解釈を持っている組み込みオペレータの通話の具体的な結果を表現されており、*未解釈機能*このような抽象プロパティとその値を表現するために使用される`length`、など追跡し、SMTソルバー約推論することができます。
他の関数呼び出しの結果は、機能上の契約によって拘束される新鮮なSMT変数で表され、型を返します。
例えば、application `x + 6` 関数の結果は、`"(+ x 6)"`で表され、コール`random1toN(10)`の結果は次のように変換される

```lisp
(declare-const _i0 Int)
(assert (and (<= 1 _i0) (<= _i0 10)))
```

|English|日本語|
| --- | --- |
|contrast|コントラスト、対比|
|instead|代わりに|
|stored|保存された|
|along|沿って|
|That way|その方法|
|correctly|正しく|

> <sup><sub>
In contrast to other values, functions are not translated into SMT-LIB representation, but are instead stored in a *function environment*.
If a function is the result of an application of a higher-order function, its local environment is stored along with its refined type.
Take, for example, the function `make_const : (x : int) → int → (z : int if z == x)`.
The result of the call `make_const(1 + 2)` is the pair `({x ↦ "(+ 1 2)"}, int → (z : int if z == x))`.
That way, when the resulting function is called, its return type contract can be translated correctly.

他の値とは対照的に、関数は、SMT-LIB表現に翻訳されないが、代わりに*関数環境*に格納されています。
関数は、高次関数を適用した結果である場合に、そのローカル環境は、その洗練された型と共に記憶されています。
例えば、関数`make_const : (x : int) → int → (z : int if z == x)`してください。コール`make_const(1 + 2)`の結果は、ペア`({x ↦ "(+ 1 2)"}, int → (z : int if z == x))`です。
そうすれば、結果として関数が呼び出されたときに、その戻り値の型の契約を正確に翻訳することができます。

|English|日本語|
| --- | --- |
|establish|構築|
|relationship|関係|
|Assuming|仮定すると、|
|must|しなければなりません|
|contract|契約|
|implies|意味し|
|contract|契約|
|contravariant|反変|
|imply|暗示する|
|covariant|共変の|
|earlier|以前|
|supertype|のスーパータイプ|
|premises|の前提|
|implication|意味合い|

> <sup><sub>
Function casts must establish a subtype relationship between two refined function types, e.g. that `a₁ → b₁ <: a₂ → b₂`.
Assuming that the base types of `a₁` and `a₂` and of `b₁` and `b₂` are equal, we must prove that the contract of `a₂` implies the contract of `a₁` (as parameter types are contravariant), and that the contract of `a₂` and the contract of `b₁` imply the contract of `b₂` (since return types are covariant).
If there are multiple parameters, the contracts of all earlier parameters of the supertype must be used as premises when checking the implication of contracts for each parameter and for the return type.
For example, to prove that the type `(x : int, y : int if y > 0) → (z : int if z == x + y)` is a subtype of `(x : int if x > 0, y : int if y > x) → (z : int if z > 0)`, we must prove 1) `x > 0 ⇒ true`, 2) `x > 0 ∧ y > x ⇒ y > 0`, and 3) `x > 0 ∧ y > x ∧ z == x + y ⇒ z > 0`.

機能キャストは例えば、2洗練された関数型、サブタイプ間の関係を確立する必要があります それ`a₁ → b₁ <: a₂ → b₂`。
仮定すると`a₁`と `a₂`との基本型`b₁`と`b₂`は、我々は（パラメータの型が反変であるため） `a₂`の契約が`a₁`の契約を意味することを証明しなければならない、等しいです、`a₂`と`b₁`の契約の契約は`b₂`の契約を意味することを（戻り値の型ので、共変です）。
複数のパラメータがある場合は、各パラメータおよび戻り値の型の契約の意味をチェックする際に、スーパータイプの以前のすべてのパラメータの契約が前提として使用する必要があります。
たとえば、タイプ `(x : int, y : int if y > 0) → (z : int if z == x + y)` は `(x : int if x > 0, y : int if y > x) → (z : int if z > 0)` のサブタイプであることを証明するために、我々は 1) `x > 0 ⇒ true` , 2) `x > 0 ∧ y > x ⇒ y > 0` , 3) `x > 0 ∧ y > x ∧ z == x + y ⇒ z > 0` を証明しなければなりません。

## Possible extensions

## 可能な拡張

|English|日本語|
| --- | --- |
|experimental|実験的|
|demonstrates|実証|
|safety|安全性|
|properties|特性|
|However|しかし、|
|improved|改善された|

> <sup><sub>
This experimental implementation demonstrates a refined type-checking algorithm that can check many software safety properties.
However, it is far from complete, and could be improved in many different ways.

この実験的な実装では、多くのソフトウェア安全性のプロパティを確認することができる洗練された型チェックアルゴリズムを示しています。
しかし、完全なものにはほど遠く、多くの異なる方法で改善することができます。

|English|日本語|
| --- | --- |
|would be|なります|
|equivalent|同等の|
|would|でしょう|
|perform|実行|
|elimination|排除、消去|
|proving|証明|
|Furthermore|さらに、|
|negated|否定|
|implication|含意|
|satisfiable|満足できます|
|contract|契約|

> <sup><sub>
A simple addition would be implementing HM type inference and refined type checking for recursive functions, which are equivalent to loops and would make the language Turing complete.
Another idea is to allow type aliases for refined types (e.g. `type nat = i : int if i ≥ 0`), and to perform a simple form of dead code elimination by proving when `if` branches cannot be taken.
Furthermore, we could use the model generated by the SMT solver the negated implication is satisfiable to extract a set of values that break the contract.

単純な加算はループと同等であり、言語チューリングが完了するだろう再帰関数のためのHM型推論と洗練された型チェックを実行することになります。
もう一つのアイデアは、洗練された種類の型の別名をできるようにすることです（例えば、 `type nat = i : int if i ≥ 0`)、および支店取ることができない場合、証明することによってデッドコード削除の簡単なフォームを実行します。
さらに、我々は否定的含意が契約を破る値のセットを抽出することが充足可能SMTソルバーによって生成されたモデルを使用することができます。

|English|日本語|
| --- | --- |
|Handling|取り扱い|
|improved|改善されました|
|would|でしょう|
|as well|同様に|
|second-order|二階|
|equivalent|等価な|
|alert|警告|
|inhabiting|居住している|

> <sup><sub>
Handling of first-class functions needs to be improved.
We would need to include functions in local environment as well, and then use the function subtype-checking algorithm to check refined function types of parameters and return types.
We would need to transform some second-order contracts into equivalent refined function types, for example `f : int → int if f(0) == 1` is equivalent to `f : (x : int) → (y : int if (if x == 0 then y == 1 else true))`, while `f : array[int] → int if f == length` is equivalent to `f : (a : array[int]) → (i : int if i == length(a))`.
Finally, it would be useful to alert the user when there can be no functions inhabiting a given function type, such as `(x : int if x > 0) → (y : int if y > x and y < 0)`.

ファーストクラスの関数の取り扱いを改善する必要があります。
我々としても、ローカル環境での機能を含める必要があり、その後、パラメータの洗練された機能の種類をチェックして、型を返すために関数のサブタイプチェックアルゴリズムを使用します。
我々は `f : array[int] → int if f == length` は `(x : int if x > 0) → (y : int if y > x and y < 0)`に相当しながら、例えば、 `f : int → int if f(0) == 1` は、 `f : (x : int) → (y : int if (if x == 0 then y == 1 else true))` に相当し、同等の洗練された機能の種類にいくつかの二次契約を変換する必要があります。

|English|日本語|
| --- | --- |
|substantial|かなりの|
|would be|なります|
|prohibit|禁止します|
|side-effects|副作用|
|determinism|決定論|
|bitvectors|ビットベクトル|
|practical|実用的|
|imperative|命令的な|

> <sup><sub>
More substantial extensions would be adding a function effect system, which would prohibit the use of functions with side-effects (such as non-determinism or I/O) in refined types, and including built-in operations for additional datatypes, such as arrays, modular integers and bitvectors, which can also be reasoned about by some SMT solvers.
To make the language practical, it would also need to support imperative features such as loops and mutable local variables and data structures.

より実質的な拡張は、そのようなアレイなどの追加データ型の組込み操作など、refined タイプであり、（例えば非決定論やI/ Oなど）の副作用を持つ関数の使用を禁止する機能効果システムを追加することになります、モジュラー整数と、いくつかのSMTソルバー約推論することができますビットベクトル。
言語は、実用的にするために、それはまた、ループおよび可変ローカル変数およびデータ構造として不可欠の機能をサポートする必要があります。

|English|日本語|
| --- | --- |
|algebraic|代数の|
|ability|能力|
|so that|そのため、|
|extract|抜粋|
|non-negative|非負|
|predicate|述語|

> <sup><sub>
A very useful extension would be to allow refined types within algebraic datatypes, for example `array[i : int if i ≥ 0]`.
This would require the ability to instantiate polymorphic types with refined base types, so that we could use `get : forall[a] (array[a], i : int) → a` to extract a non-negative value from this array.
A related idea is *predicate polymorphism* [6]: we want to support types such as `array_max : forall[p : int → bool] array[i : int if p(i)] → (k : int if p(k))`.

非常に便利な拡張機能は、例えば、`array[i : int if i ≥ 0]`のために、代数的データ型内のrefinedタイプを許可することであろう。
私たちはこの配列からの非負の値を抽出するために `get : forall[a] (array[a], i : int) → a` を使用することができるようにこれは、洗練された基本型と多形型をインスタンス化する能力を必要とするであろう。
関連したアイデアがある *述語多型* [6]：私たちは、`array_max : forall[p : int → bool] array[i : int if p(i)] → (k : int if p(k))`などの型をサポートします。

|English|日本語|
| --- | --- |
|Ideally|理想的|
|could be|かもしれません|
|having|ました、もつ|
|explicitly|明示的|
|complicated|複雑な|
| as|として、等の|
|exact|正確な|
|existential|実存的な|
|situations|状況|
|precise|正確な|
|enough|十分な|
|attempts|試み|
|specified|指定の|
|qualifiers|修飾子|
|instead|代わりに|
|weakest|最弱|
|precondition|前提条件|
|propagate|伝播する|
|might be |かもしれません|
|backwards|後方|

> <sup><sub>
Ideally, refined type-checking could be used without having the programmer explicitly annotate all parameters and return types.
However, refined type inference is complicated, as it is hard to say what is the "best" refined type for a given expression.
For example, the exact refined type of `square(random1toN(5))` is the existential type `exists[i : int if 1 ≤ i ≤ 5] i * i`, but in many situations `i : int if 1 ≤ i ≤ 25` is precise enough while being much clearer.
The Liquid Types [3] type inference system attempts to solve this by inferring refined types made only of programmer-specified qualifiers, such as `0 ≤ _` and `_ < length(_)`.
The system presented in [4] instead uses *weakest precondition generation* to propagate the conditions of a contract that might be broken backwards to the function parameters.

理想的には、洗練された型チェックは、プログラマが明示的にすべてのパラメータに注釈を付けると型を返すことなく使用することができます。
それは与えられた式のための「最良」の洗練されたタイプであることを言うのは難しいですしかし、洗練された型推論は、複雑です。
例えば、 `square(random1toN(5))` の正確な洗練されたタイプは、実存型 `exists[i : int if 1 ≤ i ≤ 5] i * i` ですが、より明確にしながら、多くの状況で `i : int if 1 ≤ i ≤ 25` は、十分に正確です。
[3]推論システムを型液状タイプは、 `0 ≤ _` と `_ < length(_)` としてのみ、プログラマが指定した修飾子で作られた洗練されたタイプを、推論することによってこの問題を解決しようとします。
[4]で提示システムではなく、関数のパラメータに後方に破壊される可能性がある契約の条件を伝播する*最も弱い前提条件の生成*を使用しています。

## References

## 参考文献

[1] K Knowles, C Flanagan. *[Hybrid Type Checking](http://www.kennknowles.com/research/knowles-flanagan.toplas.2010.pdf)*. 2006/2010

[2] K Knowles, A Tomb, J Gronski, S N Freund, C Flanagan. *[Sage: Uniﬁed Hybrid Checking for First-Class Types, General Reﬁnement Types, and Dynamic](http://sage.soe.ucsc.edu/sage-tr.pdf)*. 2006

[3] P M Rondon, M Kawaguchi, R Jhala. *[Liquid Types](http://goto.ucsd.edu/~rjhala/papers/liquid_types.pdf)*. 2008

[4] H Zhu, S Jagannathan. *[Compositional and Lightweight Dependent Type Inference for ML](https://www.cs.purdue.edu/homes/suresh/papers/vmcai13.pdf)*. 2013

[5] D Jovanović, L de Moura. *[Solving Non-Linear Arithmetic](http://research.microsoft.com/en-us/um/people/leonardo/files/IJCAR2012.pdf)*. 2012

[6] N Vazou, P M Rondon, R Jhala. *[Abstract Reﬁnement Types](http://goto.ucsd.edu/~rjhala/liquid/abstract_refinement_types.pdf)*. 2013


[heartbleed]: http://en.wikipedia.org/wiki/Heartbleed "Heartbleed"
[z3]: http://z3.codeplex.com/ "Z3, a high-performance theorem prover"
[ml-language]: http://en.wikipedia.org/wiki/ML_(programming_language) "ML programming language"
[f7]: http://research.microsoft.com/en-us/projects/f7/ "F7: Refinement Types for F#"
[f-star]: http://research.microsoft.com/en-us/projects/fstar/ "F*"
[vcc]: http://research.microsoft.com/en-us/projects/vcc/ "VCC: A Verifier for Concurrent C"
[whiley]: http://whiley.org/about/overview/ "Whiley - A Programming Language with Extended Static Checking"
[sage]: http://sage.soe.ucsc.edu/ "Sage: A Programming Language with Hybrid Type-Checking"
[smtlib]: http://www.smtlib.org/ "The Satisfiability Modulo Theories Library"
[robinson-arithmetic]: http://en.wikipedia.org/wiki/Robinson_arithmetic "Robinson arithmetic"
[algorithm-w]: https://github.com/tomprimozic/type-systems/tree/master/algorithm_w


--------

## EBNF

    ident           ::= [A-Za-z][_A-Za-z0-9]*
    int             ::= [0-9]+
    expr            ::= app_expr ":" ty opt("if" expr)
                      | "let" ident "=" expr "in" expr
                      | boolean_expr
                      | fun_expr
                      | "if" expr "then" expr "else" expr

    boolean_expr    ::= "not" relation_expr
                      | relation_expr opt(("and"|"or") relation_expr)

    relation_expr   ::= arithmetic_expr rep(relation_op arithmetic_expr)

    arithmetic_expr ::= mul_expr rep(("+"|"-") mul_expr
    mul_expr        ::= unary_expr rep(("*"|"/"|"%") unary_expr

    unary_expr      ::= "-" unary_expr
                      | app_expr

    app_expr        ::= simple_expr rep("(" repsep(expr, ",") ")")

    simple_expr     ::= ident
                      | int
                      | "true"
                      | "false"
                      | "(" expr ")"

    relation_op     ::= "<=" | ">=" | "<" | ">" | "==" | "!="

    fun_expr        ::= "fun" ident "->" expr 
                      | "fun" "(" param_list ")" opt(":" return_ty) "->" expr

    param_list      ::= repsep(param, ",")

    param           ::= ident ":" ty opt("if" expr)
                      | ident

    return_ty       ::= some_simple_ty
                      | "(" ident ":" ty ")"                  
                      | "(" ident ":" ty "if" expr ")"

    ident_list      ::= rep1(ident)

    ty_forall       ::= ty
                      | "forall" "[" ident_list "]" ty

    ty              ::= function_ty
                      | simple_ty
                      | "some" "[" ident_list "]" ty

    function_ty     ::= "(" ")" "->" function_ret_ty
                      | simple_ty "->" function_ret_ty
                      | "(" refined_ty ")" "->" function_ret_ty
                      | "(" param_ty "," param_ty_list ")" "->" function_ret_ty

    function_ret_ty ::= ty
                      | "(" refined_ty ")"

    param_ty_list   ::= rep1sep(param_ty, ",")

    param_ty        ::= refined_ty
                      | ty

    refined_ty      ::= ident ":" ty opt("if" expr)

    some_simple_ty  ::= simple_ty
                      | "some" "[" ident_list "]" simple_ty

    simple_ty       ::= ident "[" rep1sep(ty, ",") "]"
                      | ident
                      | "(" ty ")"


## ソースファイル一覧

- expr.scala データ定義
- parser.scala パーサ
- core.scala 定数定義
- infer.scala 型推論
- printing.scala 文字列出力
- refined.scala refined type
- smt.scala SMTソルバのZ3呼び出し

- test.scala テスト
- test_parser.scala 構文解析のテスト
- test_infer.scala 推論のテスト
- test_refined.scala refined typeのテスト


## 処理内容

parser.scalaで構文木の`s_expr`を読み込む。
infer.scalaで`s_expr`から`t_expr`に変換する。
refined.scalaでZ3を呼び出して、`t_expr`の依存型の情報の整合性をチェックする

