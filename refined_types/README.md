> <sup><sub>
Refined types: a better type system for more secure software

# Refined types：超安全ソフトウェア用最適型システム

<sup><sub>
Refined 洗練された
experiment 実験
combines 兼ね備え
dependent 依存
Although であるが
i.e. すなわち
contracts 契約
prove 証明する
absence ない状態
</sub></sup>

> <sup><sub>
_This_ is _another type systems experiment_ that combines _Hindley–Milner type inference_ with _static type-checking of a limited version of dependent types called *refined types*_.
Although _the type-checker_ only allows _refined types_ on _function parameters and return types (i.e. *function contracts*)_,
_it_ can prove _the absence of some of the most common software bugs_ .

これは、別な*refined types*と呼ばれる依存型の限られたバージョンのヒンドリミルナ型推論をかね揃えた静的型検査の型システムの実験です。
型検査器は(関数の契約のような)関数のパラメータと返り値の型にのみ*refined types* を書くことが出来ますが、それは多くのソフトウェアに共通したバグのいくつかがないことを証明出来ます。

<sup><sub>
consider 考慮する
division 除算
denominator 分母
Thus したがって、
as would 同じように
deduce 演繹する
potentially 潜在的に
non-deterministic 非決定的
during 間に
compilation コンパイル
</sub></sup>

> <sup><sub>
For _a simple example_, let's consider _integer division_:
_we_ know that _the denominator_ cannot be _zero_.
Thus, if _we_ define _division_ as `/ : (int, i : int if i != 0) → int`,
_the refined type-checker_ can tell us *during compilation* that `1/0` will _result in an error_,
as would _`1/(2 * 3 - 6)` and `1/(4 % 2)`_.
_The system_ can also deduce that _the program `10 / (random1toN(10) - 5)`_ is _potentially unsafe_, where `random1toN` is _a non-deterministic function_ whose _type_ is `(N : int if N ≥ 1) → (i : int if 1 ≤ i and i ≤ N)`.

シンプルな例で、整数の除算について考えてみましょう:
我々は分母がゼロに出来ない事を知ってます。
従って、もしも`/ : (int, i : int if i != 0) → int`という割算を定義すれば、Refined型検査器は`1/(2 * 3 - 6)` と `1/(4 % 2)`のような`1/0`の結果がエラーになる事をコンパイル時に我々に伝える事が出来ます。
このシステムはプログラム`10 / (random1toN(10) - 5)` が潜在的に安全はでないことを計算出来て、ここで`random1toN`は非決定的な関数で、この型は`(N : int if N ≥ 1) → (i : int if 1 ≤ i and i ≤ N)`です。

<sup><sub>
verify 確認します
accessed アクセス
out of bounds 範囲外
appropriate 適切な
contracts 契約
prevented 防止
</sub></sup>

> <sup><sub>
_Refined type checking_ can also be used _to verify_ that _arrays_ are not accessed _out of bounds_,
and using _appropriate contracts on functions `alloc` and `memcpy`_,
_software bugs such as [Heartbleed][heartbleed]_ could be prevented.

Refined型検査は配列が境界外へのアクセスされていない事の検証にも使え、
関数`alloc`と`memcpy`上で適切な契約を使い、
[Heartbleed][heartbleed]のようなソフトウェアバグを防ぐことができます。

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

<sup><sub>
actually 実際に
straightforward 簡単な
turned out 判明
simpler 単純な
Essentially 基本的に
converted 変換されました
series シリーズ
mathematical 数学の
formulas 式
validity 妥当性
assessed 評価
prover 証明器
handled 取り扱い
explained 説明
below 以下
</sub></sup>

> <sup><sub>
_The implementation of a refined type-checker_ is _actually very straightforward_  and turned out to be _much simpler than I expected_.
Essentially, _program expressions_ and _contracts on function parameters　and return types_ are converted into _a series of mathematical formulas and logical statements_,
_the validity_ of which is then assessed using _an external automated theorem prover [Z3][z3]_.
_The details of the implementation_,
including _the tricks_ that allow _functions to be handled as first-class values_,
are explained below.

Refined型検査の実装は、実際には非常に素直で、予想よりはるかに簡単であることが分かりました。
基本的に、プログラムの式や関数のパラメータと戻り値の型の契約は、数学的な式や論理ステートメントの列に変換されたもので、
その妥当性は外部自動定理証明[Z3][z3]を用いて評価されたものです。
実装の詳細は、
ファーストクラスの値として関数を扱えるようにするトリックを含んでおり、以下で説明します。

<sup><sub>
should すべきです
familiar おなじみの
its それの
</sub></sup>

> <sup><sub>
*Note about syntax:*
_These examples_ use _a syntax similar to JavaScript or TypeScript_ that should be _familiar to most programmers_, which is _different from the [ML][ml-language]-like syntax_ that _the type-checker and its test cases_ use.

*構文に関する注意:*
これらの例では多くのプログラマにとって親しみやすくする為に、JavaScriptかTypeScriptに似た構文を用いていますが、型検査やテストケースで使用している[ML][ml-language]ライクな構文とは違います。

> <sup><sub>
Overview

## 概要

<sup><sub>
i.e. すなわち
often 多くの場合
presented 提示
holy grail 至高の目標
yet まだ
despite かかわらず、
intensive 集中的な
remain 残ります
impractical 非現実的な
predicates 述語
notation 表記法
commonly 一般的に
academic アカデミック
literature 文献
</sub></sup>

> <sup><sub>
*Dependent types*, i.e. _types that depend on values_, are often presented as *the holy grail of secure static type systems*, yet despite intensive research they remain complex and impractical and are only used in research languages and mathematical proof assistants.
_*Refined types* or *contracts*_ are _a restricted form of dependent types_ that combine _base datatypes with logical predicates_;
for example, _the type of natural numbers_ could be written `x : int if x ≥ 0`
(_the notation most commonly used in academic literature_ is `{ν : int | ν ≥ 0}`).

*依存型*はすなわち値に依存する型で、安全な静的型システムの究極の目標です。しかし多くの研究者によって研究されている割にまだ複雑で非現実的な状況が続いています。そのため研究用の言語や定理証明アシスタントでのみ使用されています。

*Refinedタイプ*または*契約*は、基本型に論理的な式をつけた依存型の制限された形です;
例えば、自然数の型は`x : int if x ≥ 0`と書けます(アカデミックな文献上のよく使われている記法は`{ν : int | ν ≥ 0}`です)。

<sup><sub>
experimentation 実験
past 過去
decade 10年
Hybrid ハイブリッド
contracts 契約
deferring 延期
Limited 限られました
reduce 減らします
amount 量
prove 証明します
safety 安全性
Liquid 液状の
also また
experimental 実験的
superseded 置き換え
since 以来、
</sub></sup>

> <sup><sub>
_Refined types_ have been _a topic of a lot of research and experimentation in the past decade_.
*Hybrid type checking* [1] combines _static and dynamic type-checking_ by _verifying the contracts statically when possible_ and deferring the checks until runtime when necessary (implemented in programming language [Sage][sage] [2]).

Refined型は、過去十年間の間、多くの研究と実験の話題となっています。
*ハイブリッド型検査* [1] は、可能なら静的に契約を認証し、必要なら実行時までチェックを延期する、静的および動的な型検査を兼ね備えています(プログラミング言語[Sage][sage] [2]で実装されています)。

> <sup><sub>
*Limited automatic inference of function contracts* was developed which can reduce _the amount of type annotations necessary to prove software safety_ (e.g. Liquid Types [3] and [4]).

型注釈が必要なソフトウェア安全性の証明の量を削減出来る、制約付き関数契約の自動推論が開発されました
(例えば、Liquid型[3]と[4])。

> <sup><sub>
Refined types have also been used in some experimental programming languages and verifying compilers, such as the [VCC][vcc], a verifier for concurrent C, [F7][f7], which implements refined types for F# (since superseded by [F*][f-star]), and the [Whiley][whiley] programming language.

Refined型はまた、[VCC][vcc]、コンカレントCの検証、[F7][f7]、F#のRefine型の実装([F*][F-star]で取って代わられた)、そして[Whiley][whiley]プログラミング言語など、いくつかの実験的プログラミング言語とコンパイラ検証器で使われています。

<sup><sub>
primarily 主に
nevertheless それにもかかわらず
variety 多様
strips ストリップ
provers 証明系
SMT solvers SMTソルバ
complicated 複雑な
contracts 契約
instead 代わりに
formulas 式
</sub></sup>

> <sup><sub>
This experiment, inspired primarily by Sage and Liquid Types, is an implementation of refined type-checking for a simple functional language.
Refined types are only allowed on function parameters and return types; nevertheless, a variety of static program properties can be verified.
The type-checker first strips all refined type annotations and uses Hindley–Milner type inference to infer base types of functions and variables.
Then it translates the program into [SMT-LIB][smtlib], a language understood by automated theorem provers called *SMT solvers*.
SMT solvers understand how integers and booleans work, so simple expressions such as `1 + a` can be translated directly.

この実験は、主にSageとLiquid型からインスパイアされた、単純な関数型言語用のReined型検査の実装です。
Refined型は関数の引数と戻り値の型でのみ使用可能ですが;それでも静的プログラムの様々な性質を検証できます。
型検査器は、まずすべてのRefined型の注釈を取り除き、ヒンドリー - ミルナー型推論器を使って関数や変数の基本の型を推論します。
次に、*SMTソルバ*と呼ばれる自動定理証明で読み込める[SMT-LIB][smtlib]言語にプログラムを変換します。
SMTソルバは整数とブールを理解出来るので、単純な`1 + a`のような式は直接変換できます。

> <sup><sub>
Translation of functions is more complicated,
as SMT solvers use first-order logic and cannot handle functions as first-class values,
so the contracts on their parameters and return types are translated instead.
The resulting SMT-LIB formulas are run through a SMT solver (this implementation uses [Z3][z3]) to verify that none of the translated contracts are broken.


関数の変換はもっと複雑で、
SMTソルバーは一階論理を使い、ファーストクラスの値として関数を扱えないので、
変わりにそれらのパラメータとリターンの型の制約が変換されます。
結果のSMT-LIB式はSMTソルバーを実行して(この実装は[Z3][z3]を使用)変換された制約のいずれが壊れていないことを検証します。

> <sup><sub>
This design allows the refined type-checker to handle a variety of programming constructs, such as multiple variable definitions, nested function calls, and if statements.
It can also track abstract properties such as array length or integer ranges, and handle function subtyping.
The following examples demonstrate these features:

この設計はRefined型検査器が、複数の変数定義やネストした関数コール、if文のようなプログラミング構築など多様な処理を可能にしています。
これはまた、配列の長さや整数の範囲の抽象プロパティを追跡し、そして部分型付けを扱うことができます。
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
<sup><sub>
particularly 特に
arithmetic 算術
incomplete 不完全
undecidable 決定不能な
Although であるが
prove 証明する
within 内部で
Instead 代わりに、
Even though たとえ、にもかかわらず
decidable 決定可能な
certain 一定
disprove 反証する
equalities 等式
</sub></sup>

> <sup><sub>
The `get_2dimensional` function is particularly interesting; it uses [non-linear integer arithmetic][robinson-arithmetic], which is incomplete and undecidable.
Although Z3 can prove simple non-linear statements about integers, such as `x² ≥ 0`, it cannot prove that the array is accessed within bound in the function `get_2dimensional`.
Instead, it has to convert the formula to real arithmetic and use the NLSat solver [5].
Even though non-linear real arithmetic is complete and decidable, this approach only works for certain kinds of problems; for example, it cannot disprove equalities that have real solutions but no integer ones, such as `x³ + y³ == z³` where `x`, `y` and `z` are positive.

`get_2dimensional`関数は特に興味深く; これは[非線形整数代数][robinson-arithmetic]を使っており、不完全なため決定不能です。
Z3は、 `x² ≥ 0` のような整数についての簡単な非線形な文を証明することができますが、関数 `get_2dimensional`中の配列が範囲内でアクセスされていることを証明することはできません。
その代わりに、それはreal算術式に変換してNLSatソルバ[5]を使用する必要があります。
実際の計算が完了し、決定可能であっても非線形かかわらず、このアプローチは、ある種の問題のために動作します。
例えば、このようなそ正である`x`, `y` ,`z`について`x³ + y³ == z³`のような真の解決策が、整数のものを持っていない等式である事を反証することはできません。

> <sup><sub>
Implementation

## 実装

> <sup><sub>
Type inference

### 型推論

<sup><sub>
slightly 少し
modified 変更された
perform 実行する
instead 代わりに
merely 単に、だけで
later 後で
predicate 述語
as well 同様に
prevent 防ぐ
unexpectedly 予想に反し
propagating 伝播
</sub></sup>

> <sup><sub>
After lexing and parsing, a slightly modified [**algorithm-w**][algorithm-w] is used to perform standard Hindley-Milner unification-based type inference on the AST.
_The main difference_ is that instead of _merely inferring_ _the type of the input expression_, the algorithm also transforms _the AST into a typed expression tree_ that will be used later by the refined type-checker.
_The predicate expressions in refined function types_ have _their types_ inferred as well and unified with `bool`.
To _prevent unification_ from _unexpectedly propagating refined types_, _predicates_ are stripped from _function types_ before calling `unify` and before adding the types to the typing context.

字句解析と構文解析した後、わずかに変更された[**algorithm-w**][algorithm-w]は、ASTに標準的なヒンドリー - ミルナー単一化ベースの型推論を実行するために使用されます。
_主な違い_は、_かわりに単に入力式の型を推論すること_で、_アルゴリズム_はまた、Refined型検査器によって後で使われる_型指定された式ツリー_へ_AST_を変換します。
_Refined関数型内の述語式_は、同様に推論され、ブールで単一化された_それらの型_をもちます。
_予期しないRefined型の伝播_から_単一化を防ぐ_ために、_述語_は`unify`を呼ぶ前の、型付けコンテキストに型を追加する前に、_関数の型_から除かれます。

For example, the function cast
例えば、cast関数

```typescript
f : (x : int if x + 1 >= 0) -> int
```

<sup><sub>
algorithm アルゴリズム
roughly おおよそ
representation 表現
denotes 表します
</sub></sup>

is translated by _the type inference algorithm_ roughly into the following representation, where
`{e; τ}` denotes a typed tree node with expression `e` and type `τ`:

は_型推論アルゴリズムで_おおよそ次の表現に変換されます、ここでの`{e; τ}`は式 `e` と型 `τ` を持つ型付きツリーノードを表します：


```typescript
{
	{f; int -> int} : (x : int if {{{x; int} + {1; int}; int} >= {0; int}; bool}) -> int;
	int -> int
}
```

> <sup><sub>
Refined type-checking

### Refined 型検査

<sup><sub>
proving 証明
reasoned 筋の通った、推論
precise 正確な
interpretations 解釈
literally 文字通り
specific 特定
certain 一定
more-or-less 多かれ少なかれ
assertions の表明
</sub></sup>

> <sup><sub>
The goal of refined type-checking is *proving* that none of the function contracts can be broken at runtime.
To do this, expressions of the source program must be translated into SMT-LIB formulas, so they can be reasoned about in proofs by the SMT solver.
Some expressions, such as integer constants and applications of built-in operators (e.g. `+`, `%`, `>=`, `==` and `or`), have precise values or interpretations in SMT theories and can be translated literally.
Others, such as function parameters and the return value of a `random1toN(10)` call, don't have specific values and we can only make certain more-or-less precise assertions about them.

Refined型検査の目的は関数の契約が実行時に破壊可能ではないことを*証明すること*です。
これを行うには、ソースプログラムの式は、SMT-LIB式に変換されなければならず、SMTソルバーによって証明について推論することができます。
整数定数や組み込み演算子の適用 (例えば、 `+`、`%`、`>=`、`==`と`or`)のような、式はSMT理論内の正確な値または解釈を持っていて文字通りに変換することができます。
関数のパラメータや `random1toN(10)`呼び出しの戻り値のような、
他のものは、特定の値を持たず、我々はそれらに関して多かれ少なかれ一定の正確な表明を行うことができます。

<sup><sub>
contract 契約
satisfied 満足、満たされた
examine 調べる
during 間に
</sub></sup>

> <sup><sub>
We can use the SMT-LIB representation of an expression to check if a contract is satisfied.
For a simple example, let's examine the SMT-LIB script generated during refined type-checking of the function `test`:

我々は契約が満たされているかの検査に式のSMT-LIB表現を使用できます。
簡単な例で、関数 `test`のRefined型検査時に生成されたSMT-LIBスクリプトを調べてみましょう：

```typescript
function test(x : int if x > 3) : (z : int if z > 0) {
	return x - 2
}
```

<sup><sub>
declare 宣言する
</sub></sup>

> <sup><sub>
We first declare a new SMT-LIB variable for the parameter `x`.
Its value is unknown and the most we can say about it is that `x > 3`.

我々は、まずパラメータ `x` の新しい SMT-LIB 変数を宣言します。
その値は分かりませんが、我々はそれが `x > 3` であると言う事ができます。

```lisp
(declare-const x Int)                   ; declare `x : int`
(assert (>= (- x 1) 3))                 ; equivalent to `x > 3`
(push)                                  ; enter new stack frame
(assert (not (>= (- (- x 2) 1) 0)))     ; equivalent to `not (z > 0)` where `z == x - 2`
(check-sat)                             ; check satisfiability
(pop)                                   ; exit last stack frame
```

<sup><sub>
satisfied 満足
validity 妥当性
implication 意味合い
premises の前提
conclusion 結論
satisfiable 満足できる、充足可能な
Fortunately 幸いなことに
negating ネゲート
whether かどうか
negation 否定
satisfiable 満足できます
produces 生成
counterexample 反例
negated 否定
conclude 結論を出す
neither どちらもありません
negated 否定
incorrectly 間違って
strict 厳格な
inequalities 不等式
instead 代わりに
</sub></sup>

> <sup><sub>
To prove that a contract is satisfied, we need to prove the *validity* of _the logical implication_ where all previous formulas and assertions are premises and the contract is the conclusion.
In the above example, _the required implication_ is `x > 3 ⇒ x - 2 > 0`.
However, SMT solvers can only prove that a formula is *satisfiable* (there exists an assignment of values to the variables that makes the formula true),
not that it is *valid* (it is true for every assignment of values).

契約が成立していることを証明するために、我々は以前のすべての数式や表明が前提として、契約がまとまり、論理的意味合いの*妥当性*を証明する必要があります。
上記の例では、必要な意味合いは `x > 3 ⇒ x - 2 > 0` です。
しかしながら、SMTソルバは式が*満足する*(真の式になる、変数への値の割り当てが存在する)事は証明できますが、
それは*有効*(値のすべての割り当てで真)である事ではありません。

> <sup><sub>
Fortunately, we can determine if _the implication_ is valid by negating the condition of the contract and checking whether _the negation of the implication_ is satisfiable.

幸いなことに、意味合いの否定が満足しないかを調べる事で、我々は契約の条件の否定から意味合いが有効かどうかを決定できます。

> <sup><sub>
If _the SMT solver_ produces _a model_ showing that _it_ is, indeed, _satisfiable_,
_we_ have _a counterexample of values_ that break _the contract_.

_SMTソルバ_が_それ_が確かに_充足である_ことを示す_モデル_を作成する場合、
_我々_は_契約_を破る_値の反例_を持っています。

> <sup><sub>
If _the SMT solver_ proves that _the negated implication_ is _not satisfiable_,
_we_ conclude that _the implication itself_ is _valid_,
and that _the contract_ cannot be _broken_.

_SMTソルバ_が_否定的意味合い_は_充足でない_ことを証明した場合、
_我々_は_意味合い自体_が_有効_で、かつ_契約_を_破壊_できないと結論付けます。

> <sup><sub>
If _the solver_ can neither show that _the negated implication_ is _satisfiable_ nor prove that it is not,
_its satisfiability_ is checked again in _the theory_ of _non-linear real arithmetic_ by _the NLSat solver_.

_ソルバー_はどちらも_否定的意味合い_が_充足_であることを示していても、そうでないことの証明ではなく、
_その充足_が_NLSatソルバー_により_非線形real算術_の_理論_で再びチェックされます。

> <sup><sub>
(Z3 incorrectly translates strict inequalities when translating between the theories of integer and real arithmetic, which is why `>=` and `<=` are used instead of `>` and `<`.) 


（整数と実数算術の理論との間で変換するときZ3が間違って `>=`と `<=` ではなく`>`と`<`の使用されている理由である、厳格に不等式を変換します。）

<sup><sub>
trivially 自明に、トリビアルに
representation 表現
premises の前提
negation 否定
premises の前提
non-trivial 非自明
contract 契約
corresponding 対応する
refer 参照する
earlier 以前
so that そのため、
contract 契約
correctly 正しく
</sub></sup>

> <sup><sub>
Some expressions, such as integers, booleans and variables that do not have function types, can be trivially translated into SMT-LIB representation, but the translation of other kinds of expressions can be tricky.

関数型を持っていない整数、ブール値や変数、などのいくつかの式は、
自明なSMT-LIB 表現に変換することができますが、式の他の種類の翻訳は注意が必要です。

> <sup><sub>
When _translating an `if` expression_,
_the boolean condition_ has to be added to _the premises when checking contracts in the `then` branch_,
while _its negation_ has to be added to _the premises when checking the `else` branch_.

`if` 式を変換する場合、
ブール条件は、_`then` ブランチの契約をチェックするとき_に前提に追加する必要があり、
その否定は、_`else` ブランチをチェックするときに_に前提に追加する必要があります。

> <sup><sub>
Another _non-trivial case_ is checking function calls,
where _each argument expression_ is _translated_ and _the contract on the corresponding parameter_ must be _checked_.

別の非自明な場合は、各引数式が変換され、対応するパラメータの契約がチェックされなければならない、関数呼び出しをチェックしています。

> <sup><sub>
_As contracts on function parameters_ can refer to _earlier parameters_,
_the representations of argument expressions_ corresponding to _named parameters_ are added to _the function's *local environment*_.

関数のパラメータの契約は、以前のパラメータを参照することができ、
名前付きパラメータに対応する引数式の表現は、関数のローカル環境に追加されます。

> <sup><sub>
In the example above,
_the local environment_ when checking _the refined return type_ is `{z ↦ "(- x 2)"}`,
so that _the variable `z` in the contract expression_ is translated correctly.

上記の例では、
ローカル環境の戻り値のRefined型の検査結果は`{z ↦ "(- x 2)"}`で、
すなわち_契約式の変数 `z`_ が正しく変換される。

<sup><sub>
specifically 特に
uninterpreted 解釈されていません
whose その
tracked 追跡
reasoned 筋の通った、推論
constrained 制約
</sub></sup>

> <sup><sub>
_The results of some function calls_ are represented directly,
_specifically the results of calls of built-in operators_,
which have _standard interpretations in SMT theories_,
and *uninterpreted functions* such as `length`,
which are used to represent _abstract properties and whose values can be tracked and reasoned about by SMT solvers_.

_いくつかの関数呼び出しの結果_は直接表現され、
_特殊な組み込みオペレータの呼び出し結果_は、
SMT理論上の標準的な解釈を持ち、
`length`のような*未解釈関数*は、
_抽象プロパティとSMTソルバーによって追跡され推論することができる値_を使った表現されています。

> <sup><sub>
_The results of other function calls_ are represented by _fresh SMT variables_,
which are constrained by _the contract on the functions return type_.

_他の関数呼び出しの結果_は_新鮮なSMT変数_で表され、これは関数の返り値の型の契約によって拘束されています。

> <sup><sub>
For example, _the result of the function application `x + 6`_ is represented by `"(+ x 6)"`,
while _the result of the call `random1toN(10)`_ is translated as

例えば、`x + 6` 関数適用の結果は、`"(+ x 6)"`で表され、
`random1toN(10)`呼び出しの結果は次のように変換されます。

```lisp
(declare-const _i0 Int)
(assert (and (<= 1 _i0) (<= _i0 10)))
```

<sup><sub>
contrast コントラスト、対比
instead 代わりに
stored 保存された
along 沿って
That way その方法
correctly 正しく
</sub></sup>

> <sup><sub>
In contrast to other values,
functions are not translated into SMT-LIB representation,
but are instead stored in a *function environment*.

他の値とは対照的に、
関数は、SMT-LIB表現に翻訳されないが、
代わりに*関数環境*に格納されています。

> <sup><sub>
If a function is the result of an application of a higher-order function, its local environment is stored along with its refined type.

関数が高階関数の適用の結果である場合に、
そのローカル環境は、そのRefined型と共に記憶されています。

> <sup><sub>
Take, for example, the function `make_const : (x : int) → int → (z : int if z == x)`.

例えば、関数`make_const : (x : int) → int → (z : int if z == x)`を見ましょう。

> <sup><sub>
The result of the call `make_const(1 + 2)` is the pair `({x ↦ "(+ 1 2)"}, int → (z : int if z == x))`.

`make_const(1 + 2)`の呼び出しの結果は、ペア`({x ↦ "(+ 1 2)"}, int → (z : int if z == x))`です。

> <sup><sub>
That way, when _the resulting function_ is called, _its return type contract_ can be translated correctly.

そうすれば、関数の結果は呼び出されたときに、_その戻り値の型の契約_を正確に翻訳することができます。

<sup><sub>
establish 構築
relationship 関係
Assuming 仮定すると、
must しなければなりません
contract 契約
implies 意味し
contract 契約
contravariant 反変
imply 暗示する
covariant 共変の
earlier 以前
supertype のスーパータイプ
premises の前提
implication 意味合い
</sub></sup>

> <sup><sub>
_Function casts_ must establish _a subtype relationship between two refined function types_,
e.g. that `a₁ → b₁ <: a₂ → b₂`.

機能キャストは例えば`a₁ → b₁ <: a₂ → b₂` のような、_２つのRefined型関数のサブタイプ間の関係_を確立する必要があります。

> <sup><sub>
Assuming that _the base types_ of _`a₁` and `a₂`_ and of _`b₁` and `b₂`_ are equal_,
we must prove that _the contract of `a₂`_ implies _the contract of `a₁`_ (as parameter types are contravariant), and that _the contract of `a₂`_ and _the contract of `b₁`_ imply the contract of `b₂` (since return types are covariant).

`a₁`と `a₂`、`b₁`と`b₂`の基本型が等しいと仮定すると、
我々は_`a₂`の契約_が_`a₁`の契約_を意味し（パラメータの型が反変であるため）、
そして_`a₂`の契約_と_`b₁`の契約_は_`b₂`の契約_を意味する（戻り値の型が共変であるため）をことを証明しなければならない。

> <sup><sub>
If _there are multiple parameters_,
_the contracts of all earlier parameters of the supertype_ must be used as premises when checking _the implication of contracts_ for _each parameter_ and for _the return type_.

_複数のパラメータがある_場合、
スーパータイプのすべての以前のパラメータの契約は、各パラメータおよび戻り値の型の契約の意味合いをチェックする時に、前提として使用される必要があります。

> <sup><sub>
For example, to prove that the type `(x : int, y : int if y > 0) → (z : int if z == x + y)` is a subtype of `(x : int if x > 0, y : int if y > x) → (z : int if z > 0)`,
we must prove 1) `x > 0 ⇒ true`, 2) `x > 0 ∧ y > x ⇒ y > 0`, and 3) `x > 0 ∧ y > x ∧ z == x + y ⇒ z > 0`.

たとえば、型 `(x : int, y : int if y > 0) → (z : int if z == x + y)` が `(x : int if x > 0, y : int if y > x) → (z : int if z > 0)` のサブタイプであることを証明するために、
我々は 1) `x > 0 ⇒ true` , 2) `x > 0 ∧ y > x ⇒ y > 0` , 3) `x > 0 ∧ y > x ∧ z == x + y ⇒ z > 0` を証明しなければなりません。

> <sup><sub>
Possible extensions

## 可能な拡張

<sup><sub>
experimental 実験的
demonstrates 実証
safety 安全性
properties 特性
However しかし、
improved 改善された
</sub></sup>

> <sup><sub>
_This experimental implementation_ demonstrates a _refined type-checking algorithm_ that can check _many software safety properties_.
However, it is far from complete, and could be improved in _many different ways_.

_この実験的な実装_で、_多くのソフトウェア安全性のプロパティ_を確認することができる_Refined型検査アルゴリズム_を示しました。
しかし、完全なものにはほど遠く、_多くの異なる方法_で改善することができます。

<sup><sub>
would be なります
equivalent 同等の
would でしょう
perform 実行
elimination 排除、消去
proving 証明
Furthermore さらに
negated 否定
implication 意味合い
satisfiable 満足できます
contract 契約
</sub></sup>

> <sup><sub>
_A simple addition_ would be implementing _HM type inference and refined type checking for recursive functions_, which are equivalent _to loops_ and would make _the language Turing complete_.
_Another idea_ is _to allow type aliases for refined types_ (e.g. `type nat = i : int if i ≥ 0`),
and to perform _a simple form of dead code elimination by proving_ when _`if` branches_ cannot be taken.
Furthermore, we could use _the model generated by the SMT solver_ the _negated implication_ is satisfiable to extract _a set of values that break the contract_.

_単純な加算_はループと同等であり、チューリング完全な言語である、_再帰関数のためのHM型推論とRefined型検査_を実行することになります。
_もう一つのアイデア_は、_Refined型の別名を許可する事_で（例えば、 `type nat = i : int if i ≥ 0`)、
および_`if`ブランチ_が実行されない場合を_証明することによってデッドコード削除をする簡単なフォーム_を実行します。
さらに、我々は_SMTソルバーによって生成されたモデルの否定的意味合い_が_契約を破る値の集合_を抽出することが充足可能である事を使用できます。

<sup><sub>
Handling 取り扱い
improved 改善されました
would でしょう
as well 同様に
second-order 二階
equivalent 等価な
alert 警告
inhabiting 居住している
</sub></sup>

> <sup><sub>
_Handling of first-class functions_ needs to be improved.
We would need to include _functions in local environment_ as well, and then use _the function subtype-checking algorithm_ to check _refined function types of parameters_ and return types.
We would need to transform _some second-order contracts_ into _equivalent refined function types_, for example `f : int → int if f(0) == 1` is equivalent to `f : (x : int) → (y : int if (if x == 0 then y == 1 else true))`, while `f : array[int] → int if f == length` is equivalent to `f : (a : array[int]) → (i : int if i == length(a))`.
Finally, it would be useful to alert the user when there can be no functions inhabiting a given function type, such as `(x : int if x > 0) → (y : int if y > x and y < 0)`.

_ファーストクラスの関数の取り扱い_を改善する必要があります。
我々としても、_ローカル環境での関数_を含める必要があり、その後、_パラメータのRefined関数型_を検査して、型を返すために_関数のサブタイプチェックアルゴリズム_を使用します。
我々は `f : array[int] → int if f == length` は `(x : int if x > 0) → (y : int if y > x and y < 0)`に相当しながら、例えば、 `f : int → int if f(0) == 1` は、 `f : (x : int) → (y : int if (if x == 0 then y == 1 else true))` に相当し、_同等のRefined関数型_に_いくつかの二階の契約_を変換する必要があります。

<sup><sub>
substantial かなりの
would be なります
prohibit 禁止します
side-effects 副作用
determinism 決定論
bitvectors ビットベクトル
practical 実用的
imperative 命令的な
</sub></sup>

> <sup><sub>
_More substantial extensions_ would be adding _a function effect system_, which would prohibit _the use of functions with side-effects_ (such as non-determinism or I/O) in _refined types_,
and including _built-in operations for additional datatypes_,
such as arrays, modular integers and bitvectors,
which can also be reasoned about by some SMT solvers.
To make the language practical, it would also need to _support imperative features_ such as _loops and mutable local variables and data structures_.

_より実質的な拡張_は、_関数効果システム_を追加することで、
refined型内(例えば非決定的な計算やI/Oのような)の_副作用を持つ関数の使用_を禁止し、
そして、配列、モジュラー整数とビットベクトルのような、データ型の組込み操作を含めます、
いくつかのSMTソルバーは推論することができます。
実用的言語にするために、それはまた、_ループおよび可変ローカル変数およびデータ構造_のような命令的な機能をサポートする必要があります。

<sup><sub>
algebraic 代数の
ability 能力
so that そのため
extract 抜粋
non-negative 非負
predicate 述語
</sub></sup>

> <sup><sub>
_A very useful extension_ would be to allow _refined types within algebraic datatypes_, for example `array[i : int if i ≥ 0]`.
This would require the ability to instantiate polymorphic types with refined base types, so that we could use `get : forall[a] (array[a], i : int) → a` to extract a non-negative value from this array.
A related idea is *predicate polymorphism* [6]: we want to support types such as `array_max : forall[p : int → bool] array[i : int if p(i)] → (k : int if p(k))`.

非常に便利な拡張機能は、、代数的データ型内のRefined型を許可することでしょう、例えば`array[i : int if i ≥ 0]`。
我々はこの配列からの非負の値を抽出するために `get : forall[a] (array[a], i : int) → a` を使用することができるようにこれは、Refined基本型と多相型を具体化する能力を必要となるでしょう。
関連したアイデアは *述語多相型* [6]です：我々は、`array_max : forall[p : int → bool] array[i : int if p(i)] → (k : int if p(k))`のような型をサポートしたいところです。

<sup><sub>
Ideally 理想的
could be かもしれません
having ました、もつ
explicitly 明示的
complicated 複雑な
as として、等の
exact 正確な
existential 実存的な
situations 状況
precise 正確な
enough 十分な
attempts 試み
specified 指定の
qualifiers 修飾子
instead 代わりに
weakest 最弱
precondition 前提条件
propagate 伝播する
might be かもしれません
backwards 後方
</sub></sup>

> <sup><sub>
Ideally, _refined type-checking_ could be used without having _the programmer explicitly annotate all parameters and return types_.
However, refined type inference is complicated, as it is hard to say what is the "best" refined type for a given expression.
For example, the exact refined type of `square(random1toN(5))` is the existential type `exists[i : int if 1 ≤ i ≤ 5] i * i`, but in many situations `i : int if 1 ≤ i ≤ 25` is precise enough while being much clearer.
_The Liquid Types [3] type inference system_ attempts to solve _this by inferring refined types made only of programmer-specified qualifiers_, such as `0 ≤ _` and `_ < length(_)`.
_The system presented in [4]_ instead uses *weakest precondition generation* to propagate _the conditions of a contract_ that might be broken backwards to _the function parameters_.

理想的には、Refined型検査は、_プログラマが明示的にすべてのパラメータとリターンの型に注釈を付ける_ことなく使用することができるとよいです。
しかし、Refined型推論は複雑で、与えられた式の「最良」のRefined型が何かというのは難しいのです。
例えば、 `square(random1toN(5))` の正確なRefined型は、実存型 `exists[i : int if 1 ≤ i ≤ 5] i * i` ですが、より明確にしながら、多くの状況で `i : int if 1 ≤ i ≤ 25` は、十分に正確です。
Liquid型[3]型推論システムは、 `0 ≤ _` や `_ < length(_)` のような、_プログラマが指定した修飾子で作られたRefined型のみを、推論することで_問題を解決しようとします。
[4]で提示システムでは、_関数パラメータ_に後方で破壊されるかもしれない_契約の条件_を伝播する *最も弱い前提条件の生成*をかわりに使用しています。


> <sup><sub>
References

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

