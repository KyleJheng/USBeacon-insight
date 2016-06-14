<?php
$link = mysql_connect("localhost", "root", "mikelin@206");
mysql_query("SET NAMES utf8");
mysql_select_db("oogym", $link);
$sql = mysql_query("select * from gym_member ", $link);
while($row = mysql_fetch_assoc($sql))
$output[] = $row;
print(json_encode($output));
mysql_close();
?>
