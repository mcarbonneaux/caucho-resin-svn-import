<?php

require_once "WEB-INF/php/inc.php";
require_once 'pdfGraph.php';

define("ROW1",     450);
define("ROW2",     100);
define("COL1",     50);
define("COL2",     305);
define("GRAPH_SIZE", new Size(400, 250));

define("STEP", 1);

define("HOUR", 3600);


if (! admin_init_no_output()) {
  debug("Failed to load admin, die");
  return;
} else {
    debug("admin_init successful");
}

initPDF();
startDoc();


$mbean_server = $g_mbean_server;
$resin = $g_resin;
$server = $g_server;



if ($g_mbean_server)
  $stat = $g_mbean_server->lookup("resin:type=StatService");

if (! $stat) {
  debug("Postmortem analysis:: requires Resin Professional and a <resin:StatService/> and <resin:LogService/> defined in
  the resin.xml.");
    return;
}

if (! $pdf_name) {
  $pdf_name = "Summary-PDF";
}

$mPage = getMeterGraphPage("Summary-PDF");
$pageName = $mPage->name;
$period = $_REQUEST['period'] ? (int) $_REQUEST['period'] : ($mPage->period/1000); 

if ($period < HOUR) {
	$majorTicks = HOUR / 6;
}  elseif ($period >= HOUR && $period < 3 * HOUR) {
		$majorTicks = HOUR /2;
} elseif ($period >= 3 * HOUR && $period < 6 * HOUR) {
		$majorTicks = HOUR;
} elseif ($period >= 6 * HOUR && $period < 12 * HOUR) {
		$majorTicks = 2 * HOUR;
} elseif ($period >= 12 * HOUR && $period < 24 * HOUR) {
		$majorTicks = 4 * HOUR;
} else {
		$majorTicks = 24 * HOUR;
}

$majorTicks = $majorTicks * 1000;

$minorTicks = $majorTicks/2;




$canvas->setFont("Helvetica-Bold", 26);
$canvas->writeText(new Point(175,800), "$pageName");

$page = 0;

$index = $g_server->SelfServer->ClusterIndex;
$si = sprintf("%02d", $index);

$time = (int) (time());

$end = $time;
$start = $end - $period;

$restart_time = $end;

$start = $end - $period;

$canvas->setFont("Helvetica-Bold", 16);
$canvas->writeText(new Point(175,775), "Time at " . date("Y-m-d H:i", $time));


$full_names = $stat->statisticsNames();

$statList = array();

foreach ($full_names as $full_name)  {
  array_push($statList,new Stat($full_name)) 
}


$canvas->setColor($black);
$canvas->moveTo(new Point(0, 770));
$canvas->lineTo(new Point(595, 770));
$canvas->stroke();



writeFooter();


$graphs = $mPage->getMeterGraphs();

$index=0;
foreach ($graphs as $graphData) {

	$index++;
	$x = COL1;
	
	if ($index%2==0) {
		$y = ROW2;
	} else {
		$y = ROW1;
	}
	$meterNames = $graphData->getMeterNames();
	$gds = getStatDataForGraphByMeterNames($meterNames);
	$gd = getDominantGraphData($gds);
	$graph = createGraph($graphData->getName(), $gd, new Point($x,$y));
	drawLines($gds, $graph);
	$graph->drawLegends($gds);
	$graph->end();

	if ($index%2==0) {
		$pdf->end_page();
		$pdf->begin_page(595, 842);
		writeFooter();
	}

}
 

$pdf->end_page();
$pdf->end_document();

$document = $pdf->get_buffer();
$length = strlen($document);



$filename = "$pageName" . ".pdf";



header("Content-Type:application/pdf");



header("Content-Length:" . $length);



header("Content-Disposition:inline; filename=" . $filename);



echo($document);



unset($document);



pdf_delete($pdf);

// needed for PdfReport health action
return "ok";

?>
