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

var spk = new SpeechSynthesisUtterance();

function jp(s,next) {
	spk.lang = 'ja-JP';
	spk.text=s;
	spk.rate = 1.8;
	spk.onend = next;
	speechSynthesis.speak(spk);	
}

function en(s,next) {
	spk.rate = 1.0;
	spk.lang = 'en-US';
	spk.text=s;
	spk.onend = next;
	speechSynthesis.speak(spk);	
}

function makeAnswers(words, no, size) {
	var ns = [];
	for(var i = 0; i < words.length; i++)
		if(no != i) ns.push(i);
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
  .controller('TestController', function($scope,$timeout) {
    $scope.names = names;
    $scope.namev = name;
    $scope.shuffle = false;
    $scope.size = 2;
    $scope.stopp = function(){};

    $scope.reset=function(a){
    	if(a != null) {
    		if(a==$scope.no)
    			$scope.ok++;
    		else
    			$scope.ng++;
	    	$scope.no++;
    	} else {
    		$scope.stopp();
    		$scope.no = 0;
		    $scope.ok = 0;
		    $scope.ng = 0;
		    $scope.spe = "wait";
			var words = [];
			for(var i in datas) {
				words.push({q:i, ans:datas[i]});
			}

			function shuffle(array) {
				array.sort(function(){return Math.random()-.5;});
			}
			if($scope.shuffle) shuffle(words);

			(function(){
				var stop = false;
				var sp=0;
				function speech(){
					$scope.spe = (sp+1)+"."+words[sp].q;
					if (stop) return;
					en(words[sp].no+" "+words[sp].q+" "+words[sp].q+" "+words[sp].q,function(){
						$timeout(function(){
							if (stop) return;
							$scope.spe = (sp+1)+"."+words[sp].q+" "+words[sp].ans;
							jp(words[sp].ans, function(){
								$timeout(function(){
									if (stop) return;
									sp = sp + 1
									if(words.length <= sp) {
										sp = 0;
										jp("くりかえします", function(){$timeout(speech,1000)});
									} else {

										$timeout(speech);
									}

								})
							});

						});
					})
				}
				$scope.stopp=function(){
					stop=true;
				};
				speech();

			}());


		    var n = 1;
			for(var i in words) {
				words[i].answers = makeAnswers(words,i, $scope.size);
				words[i].ok = true;
				words[i].no = n++;
			}
		    $scope.words=words;
		    $scope.word = words[0];
       	}


    	$scope.answers = makeAnswers($scope.words,$scope.no, $scope.size);
    };

    $scope.sub=function() {
    	var aa = [];
    	for(var i = 0; i < $scope.words.length;i++) {
    		$scope.words[i].ok = ($scope.words[i].radio!=i);
    	}
    	return false;
    }
    $scope.send=function() {
    	window.location = "?name="+encodeURIComponent($scope.namev);
    }
    $scope.sizes = [];
    for(var i = 0; i < 5; i++) $scope.sizes.push(i+1);
    $scope.reset();
});
</script>
<body >
<div ng-app="testApp">
	<div ng-controller="TestController">
		{{spe}}<br/>
		問題<select ng-change="send()" ng-model="namev" ng-options="nam for nam in names"></select>
		<button ng-click="reset()">リセット</button>
		<input type="checkbox" ng-model="shuffle" ng-change="reset()">シャッフル
		選択数
		<select ng-change="reset()" ng-model="size" ng-options="s for s in sizes"></select>
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
