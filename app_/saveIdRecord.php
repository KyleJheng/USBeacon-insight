<?php
$fm_number = $_GET['ar_fm_number'];
$enter_date = $_GET['ar_enter_date'];
$leave_date = $_GET['ar_leave_date'];


require 'mysql_connect.php';
mysql_query("insert into gym_access_record(ar_fm_number, ar_enter_date, ar_leave_date) values ('$fm_number', '$enter_date', '$leave_date')");
?>
