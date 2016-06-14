<?php
$UUID = $_GET['UUID'];
$user = $_GET['user'];
$lod = $_GET['log'];


require 'mysql_connect.php';
mysql_query("insert into Erecord(UUID, user, log) values ('$UUID', '$user', '$log')");
?>