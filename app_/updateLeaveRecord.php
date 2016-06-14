<?php
$fm_number = $_GET['ar_fm_number'];
$enter_date = $_GET['ar_enter_date'];
$leave_date = $_GET['ar_leave_date'];


require 'mysql_connect.php';
mysql_query("update gym_access_record set ar_leave_date = '$leave_date' where ar_fm_number ='$fm_number' and ar_enter_date ='$enter_date'");
?>
