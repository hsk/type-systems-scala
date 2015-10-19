# An Efficient Non-Moving Garbage Collector for Functional Languages

以下の適当な翻訳です。

http://www.pllab.riec.tohoku.ac.jp/papers/icfp2011UenoOhoriOtomoAuthorVersion.pdf


Katsuhiro Ueno Atsushi Ohori Toshiaki Otomo ∗

Research Institute of Electrical Communication

Tohoku University

{katsu, ohori, o-toshi}@riec.tohoku.ac.jp

## Abstract

<sup><sub>
inter-operate 相互運用
efficient 効率的な
consist of 構成される
exponentially 指数関数的に
increasing 増加
Actual 現実の
reclaimed 再要求
maintains 維持
</sub></sup>

> <sup><sub>
Motivated by _developing a memory management system_ that allows _functional languages to seamlessly inter-operate with C_,
_we_ propose _an efficient non-moving garbage collection algorithm based on bitmap marking_ and report _its implementation and performance evaluation_.

_メモリ管理システムの開発_の目的は、_関数型言語とC言語のシームレスな相互運用_を可能にする事で、
_我々_は_効率的なbitmap markingwベースとしたnon-movingガーベジコレクションアルゴリズム_を提案し、そして_実装とパフォーマンスの評価_を報告します。

> <sup><sub>
In _our method_,
_the heap_ consists of _sub-heaps {Hi | c ≤ i ≤ B} of exponentially increasing allocation sizes (Hi for 2i bytes)_ and _a special sub-heap for exceptionally large objects_.

_我々の手法_では、_ヒープ_は_指数関数的に増加するアロケーションサイズ(2iバイトのHi)の複数のサブヒープ{Hi | c ≤ i ≤ B}_ と_指数関数的に大きなオブジェクト用の特殊なサブヒープ_で構成されます。

> <sup><sub>
_Actual space for each sub-heap_ is _dynamically allocated_ and reclaimed from _a pool of fixed size allocation segments_.

_各サブヒープの実際の空間_は_動的に確保され_、そして_アロケーションセグメントのサイズにあわせたプール_から要求されたものです。

> <sup><sub>
In _each allocation segment_,
_the algorithm_ maintains _a bitmap representing the set of live objects_.

_各々のアロケーションセグメント内_では、
_アルゴリズム_は_生存オブジェクトの集合のビットマップ表現_を維持します。

> <sup><sub>
_Allocation_ is done by _searching for the next free bit in the bitmap_.

_アロケーション_は_ビットマップ中の次のフリーなビットから検索することにより_行われます。

<sup><sub>
summarize 要約する
hierarchically 階層的に
hierarchy 階層
significant 重要な
beneficial 有益
</sub></sup>

> <sup><sub>
By adding _meta-level bitmaps_ that summarize _the contents of bitmaps hierarchically_ and maintaining _the current bit position in the bitmap hierarchy_,
_the next free bit_ can be found in _a small constant time for most cases_,
and in _log32(segmentSize) time in the worst case on a 32-bit architecture_.

_ビットマップ階層のコンテンツ_をサマライズし、そして_ビットマップ階層の現在ビット位置_を維持する事で _メタレベルビットマップ_を追加し、
_次のフリーなビット_の検索は_多くの場合小さな定数時間_で終わり、
_32bitアーキティクチャ上のワーストケースではlog32(セグメントサイズ)時間_で検索可能です。

> <sup><sub>
_The collection_ is done by _clearing the bitmaps and tracing live objects_.

_コレクション_は_ビットマップのクリアと生存オブジェクトのトレーシング_によって行われます。

> <sup><sub>
_The algorithm_ can be extended to _generational GC by maintaining multiple bitmaps for the same heap space_.

_アルゴリズム_ は_同じようなヒープスペースの管理する複数ビットマップ事により世代別GC_へ拡張出来ます。

> <sup><sub>
_The proposed method_ does not require _compaction_ and _objects_ are not moved _at all_. _This property_ is _significant for a functional language to inter-operate with C_,
and _it_ should also be _beneficial in supporting multiple native threads_.

_提案手法_ では _コンパクション_は必要とせず、_オブジェクト_は_全く_動かしません。_この性質_は _関数型言語とC言語の相互運用をするために重要_で、 _ネイティブなマルチスレッドのサポートをする上では_なくてはなりません。

> <sup><sub>
_The proposed method_ has been implemented in _a full-scale Standard ML compiler_.

_提案手法_は_フルスケールのStandard ML コンパイラ_で実装されています。

> <sup><sub>
_Our benchmark tests_ show that _our nonmoving collector_ performs as efficiently as _a generational copying collector designed for functional languages_.

_我々のベンチマークテスト_では_我々のnonmoving コレクタ_は_関数型言語用に設計された世代別コピーコレクタ_と同様の効率的なパフォーマンスであることを示しています。

## 1. Introduction

<sup><sub>
interoperability 相互運用性
partially 部分的に
achieved 達成
as well as 及び
therefore 故に
</sub></sup>

> <sup><sub>
_The general motivation of this research_ is _to develop a memory management system for a functional language_ that supports _seamless interoperability with the C language_.

_この研究の一般的モチベーション_は_C言語とのシームレスな相互運用_をサポートした_関数型言語用のメモリ管理システムの開発_です。

> <sup><sub>
_We_ have partially achieved _this goal through type-directed compilation for natural data representations in ML [25]_.

_我々_はこの目標を_MLでの自然なデータ表現の型の直接的なコンパイル[25]_ で部分的に達成しました。

> <sup><sub>
Under _this scheme_, _ML records and arrays as well as atomic types such as int and real_ have _the same representation as in C_ and are _therefore directly read or updated by a C program without any data representation conversion_.

_このスキーム_下では、_MLレコードと配列及びintやreal等のアトミックなタイプ_は_Cと同じ表現_をもち、そして_データ表現の変換無しに、Cプログラムから直接的に読み込め、更新_できます。

<sup><sub>
embodied 具体化
fragment 断片
distributed 分散型の
</sub></sup>

> <sup><sub>
_This_ has been embodied in _our SML# compiler [33]_.

これは、_SML# コンパイラ[33]_で具体化されています。

> <sup><sub>
In _this implementation_, _one_ can directly import _a C function_ and call _it with data structures such as records, mutable arrays, and function closures (for call-backs) created in SML#_.

_この実装_の中で、_Cの関数_は直接インポート出来て、_レコード、更新可能な配列、SML#で作成されたコールバック用の関数クロージャのようなデータ構造_を呼び出せます。

> <sup><sub>
_The following code_ is _a fragment of a demo program distributed with SML#_.

_以下のコード_は_SML#のデモプログラムのディストリビューション内の断片_です。

```
val glNormal3dv =
    dlsym (libgl, "glNormal3dv")
    : _import (real * real * real) -> unit
...
  map (fn (vec, ...) => (glNormal3dv vec; ...))
    [((1.0, 0.0, 0.0), ...), ...]
```

<sup><sub>
precision 精密
correctly 正しく
indicating 示します
whether かどうか
exactly 正確に
adopting 採用
should be able to achieve 達成することができるはずです
interoperability 相互運用性
</sub></sup>

> <sup><sub>
_This code_ dynamically links _glNormal3dv function in the OpenGL library as an ML function of type real * real * real -> unit_ and uses _it with other SML# code_.

_このコード_は_型 real * real * real -> unit のML関数であるOpenGLライブラリ内のglNormal3dv関数_を直接リンクし、 そして_他のSML#コードで_使います。

> <sup><sub>
_SML#_ compiles _(1.0, 0.0, 0.0)_ to _a pointer to a heap block containing 3 double precision floating point numbers_.

_SML#_は_(1.0, 0.0, 0.0)_を3つのdoubleにコンパイルします。

> <sup><sub>
Since _this record_ has _the same representation as a double array of C_, _glNormal3dv_ works _correctly_.

なぜなら_レコード_が_Cのdouble配列と同じ表現_持っているため、_glNormal3dv_は_正確に_働きます。

> <sup><sub>
In _addition to this natural data representation_, _each SML# heap object_ has _a header containing object layout information indicating_ whether _each field_ is _a pointer or not_.

_この自然なデータ表現の追加_により、_各SML#ヒープオブジェクト_は_各フィールド_が_ポインタかそうでないか_を_示すオブジェクトレイアウト情報を含んだヘッダ_を持っています。

> <sup><sub>
_This information_ is used _by the SML# garbage collector (GC) to exactly trace live objects_.

_この情報_は_正確に行きているオブジェクトをトレースするためにSML#ガーベジコレクター(GC)によって_使われています。

> <sup><sub>
By _adopting the strategy_ that _the SML# GC_ only traces and collects _SML# objects_ and leaves _management of C allocated objects to C code_, _we_ should be able to achieve _seamless interoperability between SML# and C_.

_SML# GC_が_SML#のオブジェクト_をトレースしコレクトし、そして_Cコード用にCがアロケートしたオブジェクトの管理_を残す戦略を採用する事で、我々は_シームレスなSML#とCの相互運用を_達成することができるはずです。

## 2. The bitmap marking GC

<sup><sub>
so far これまでの
as well as だけでなく
</sub></sup>

> <sup><sub>
_The solution so far_ is however _only partial_ in that _data structures_ that are passed to _foreign functions_ must be allocated in _a special non-moving area_.

_これまでのソリューション_はしかし_外部関数_は_特別なnon-movingエリア_内にアロケーションされる必用があることから_データ構造_の中の部分的なもののみでした。

> <sup><sub>
_SML# compiler_, _as well as most other functional language compilers_, has used _copying garbage collection (GC) based on the commonly accepted belief being_ that, _for functional programs requiring large amount of short-lived data_, _the Cheney’s copying collector [8]_ would be _the most efficient for their minor collection_.

_SML#コンパイラ_、_だけでなく多くの他の関数型言語コンパイラ_、は持っています。

> <sup><sub>
However, _any (precise) copying GC_ requires that _the runtime system_ must be able _to locate and update all the pointers to each heap allocated data_.

しかしながら、多くの(正確な)コピーGCは_ランタイムシステム_が_ヒープアロケーションされたデータの全てのポインター再配置と更新_が可能である事が必要です。

> <sup><sub>
_This prohibits_ functional programs from _inter-operating with foreign functions or any programs_ that use _local memory space_ not accessible from _the garbage collector_.

> <sup><sub>
To _side-step this problem_, _the programmer_ must explicitly request _GC_ not to move _those data_ that are passed to _external code_.

> <sup><sub>
_This “object pinning” approach_ is not only _cumbersome_ but also _dangerous_.

> <sup><sub>
_This problem_ should be _painfully familiar to anyone_ who has tried _to write a functional program_ that interacts with _a foreign library_ that requires _callbacks or locally stores object pointers passed from the caller_.

> <sup><sub>
For _a language with rather limited interoperability_,
_“object pinning”_ might be performed _automatically_,
but for _an MLstyle language_ that provides _seamless interoperability, automatic “object pinning”_ is _difficult, if not impossible_.

> <sup><sub>
To _see the problem_,
_suppose a C function_ is called with _an array and a call-back function_.

> <sup><sub>
Since _both the C function and the call-back function_ can freely mutate _the array_,
_the runtime system_ can only safely estimate that _the set of reachable objects from the array_ passed to _the C function_ to be _the set of all the live objects in the entire heap_, including even those that may be created later by _the call-back function_.

### 2.1 The GC-mutator interface

### 2.2 The structure of the heap space and allocation strategy

### 2.3 The allocation strategy

### 2.4 The allocation algorithm

### 2.5 The bitmap marking GC algorithm

## 3. Generational extension

### 3.1 General strategy

### 3.2 The GC algorithm

## 4. Implementation and Evaluation

### 4.1 Implementation

### 4.2 Performance Evaluation

## 5. Related works

## 6. Conclusions and further development
