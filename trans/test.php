<?php
function globRecursive($path, $find) {
	$rc = array();
    $dh = opendir($path);
    while (($file = readdir($dh)) !== false) {
        if (substr($file, 0, 1) == '.') continue;
        $rfile = "{$path}/{$file}";
        if (is_dir($rfile))
            foreach (globRecursive($rfile, $find) as $ret)
            	$rc[] = $ret;
        else if (fnmatch($find, $file)) $rc[] = $rfile;
    }
    closedir($dh);
    return $rc;
}

$names = globRecursive("..", "*.md");

sort($names);

$name = isset($_GET["name"]) ? $_GET["name"] : "../README.md";
$file = file_get_contents($name);

$out = array();
//echo $file;

preg_replace_callback("/\\n<sup><sub>\\n((.*\\n)+?)<\\/sub><\\/sup>\\n/",function($m){
  global $out;
  $data = explode("\n", rtrim($m[1],"\n"));

  foreach($data as $d) {


    if(preg_match("/([\\w\\- +*,.'\"`’]+) (.*)/", $d, $m)>0){
	    $out[$m[1]] = $m[2];

    } else {
    	echo $d;
    }
  }
  return "";
}, $file);
?>



<!doctype html>
<html lang="ja">
<head>
  <meta charset="UTF-8">
  <title>Markdown test</title>
  <script src="http://ajax.googleapis.com/ajax/libs/angularjs/1.3.0-beta.5/angular.min.js"></script>
<script>
var name = <?php echo json_encode($name, JSON_UNESCAPED_UNICODE) ?>;
var names = <?php echo json_encode($names, JSON_UNESCAPED_UNICODE) ?>;
var datas = <?php echo json_encode($out, JSON_UNESCAPED_UNICODE) ?>;
var words = [];
for(var i in datas) {
	words.push({q:i, ans:datas[i]});
}

function makeAnswers(words, no) {
	var ns = [];
	for(var i = 0; i < words.length; i++)
		if(no != i) ns.push(i);
	var size = 2;
	var answers = [];
	for(var i = 0; i < size; i++) {
		var n = Math.floor(Math.random()*(ns.length));
		answers.push(ns[n]);
		ns.splice(n,1);
	}
	var n = Math.floor(Math.random()*size);
	answers.splice(n,1,no);
	return answers;
}
angular.module('testApp', [])
  .controller('TestController', function($scope) {
    $scope.no = -1;
    $scope.words=words;
    $scope.ok = 0;
    $scope.ng = 0;
    $scope.names = names;
    $scope.namev = name;
    var n = 1;
	for(var i in words) {
		words[i].answers = makeAnswers(words,i);
		words[i].ok = true;
		words[i].no = n++;
	}
    $scope.reset=function(a){
    	if(a != null) {
    		if(a==$scope.no)
    			$scope.ok++;
    		else
    			$scope.ng++;
    	}
    	$scope.no++;

    	$scope.answers = makeAnswers(words,$scope.no);
    };
    $scope.word = words[0];

    $scope.sub=function() {
    	var aa = [];
    	for(var i = 0; i < words.length;i++) {
    		words[i].ok = (words[i].radio!=i);
    	}
    	return false;
    }
    $scope.send=function() {
    	window.location = "?name="+encodeURIComponent($scope.namev);
    }
    $scope.reset();
});
</script>
<body >
<div ng-app="testApp">
	<div ng-controller="TestController">

		<select ng-change="send()" ng-model="namev" ng-options="nam for nam in names"></select>
		<div>単語練習<span>全{{words.length}}問</span></div>

		<div>第{{no+1}}問目 {{ok}}問正解 {{ng}}問不正解</div>
		<div>「{{words[no].q}}」の意味は？</div>
		<span ng-repeat="a in answers">
		<button ng-click="reset(a)">{{words[a].ans}}</button>
		</span>
		<form  ng-submit="sub()">
		<div ng-repeat="q in words">
			{{q.no}}.
			{{q.q}} <span ng-repeat="a in q.answers">
			<input type="radio" ng-model="q.radio" value="{{a}}" ng-change="sub()">
				{{words[a].ans}}</span>
			<span ng-show="!q.ok" style="color:red">○</span>
			<span ng-show="q.ok" style="color:blue">×</span>
		</div>
		</form>
</div>
</body>
</html>
