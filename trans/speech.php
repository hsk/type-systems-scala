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
function getProbrems($file) {
  $out = array();

  preg_replace_callback("/\\n<sup><sub>\\n((.*\\n)+?)<\\/sub><\\/sup>\\n/",function($m)use(&$out){
    $data = explode("\n", rtrim($m[1],"\n"));

    foreach($data as $d) {
      if(preg_match("/([\\w\\- +*:,.'\"`’]+) (.*)/", $d, $m)>0){
        $out[$m[1]] = $m[2];
      } else {
        echo $d;
      }
    }
    return "";
  }, $file);
  return $out;
}

$names = globRecursive("..", "*.md");

sort($names);
$rep = 3;
$n = 1;
while(true){
  exec('say '.$n++.'回目');

  foreach($names as $name) {
    exec('say -v "Vicki" "'.$name.'"');
    $out = getProbrems(file_get_contents($name));
    foreach($out as $e=>$j) {
      echo $e;
      $ee = "";
      for($i = 0; $i < $rep; $i++){
        $ee .= $e." ";
      }
      exec('say -v "Vicki" "'.$ee.'"');
      exec('say  "'.$j.'"');
      echo " $j\n";
    }
  }
  exec('say -v "Vicki" repeat');
}
?>
