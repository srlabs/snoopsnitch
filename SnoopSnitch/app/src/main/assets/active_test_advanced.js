var currentNetworkOperator = "";
function setNetworkOperatorName(operatorName){
	if(operatorName != currentNetworkOperator){
		document.getElementById("network_info").innerHTML = "Network: " + operatorName;
		currentNetworkOperator = operatorName;
	}
}
var currentBucket = undefined;
function setCurrentBucket(newCurrentBucket){
	if(currentBucket && document.getElementById(currentBucket))
		document.getElementById(currentBucket).className = undefined;
	currentBucket = newCurrentBucket;
	document.getElementById(currentBucket).className = "current"
}
function setGsmActive(){
	document.getElementById("advanced_header_GSM").className = "header_active";
	document.getElementById("advanced_header_3G").className = "header_inactive";
	document.getElementById("advanced_header_LTE").className = "header_inactive";
}
function set3GActive(){
	document.getElementById("advanced_header_GSM").className = "header_inactive";
	document.getElementById("advanced_header_3G").className = "header_active";
	document.getElementById("advanced_header_LTE").className = "header_inactive";
}
function setLTEActive(){
	document.getElementById("advanced_header_GSM").className = "header_inactive";
	document.getElementById("advanced_header_3G").className = "header_inactive";
	document.getElementById("advanced_header_LTE").className = "header_active";
}
var currentBuckets = {};
function updateBuckets(newBuckets){
	for(var key in newBuckets){
		if(!(key in currentBuckets && currentBuckets[key] == newBuckets[key])){
			currentBuckets[key] = newBuckets[key];
			var html;
			if(key.substr(-7) == "success")
				html = "+" + newBuckets[key] + "&nbsp;&nbsp;";
			else
				html = "-" + newBuckets[key];
			document.getElementById(key).innerHTML = html;
		}
	}
}
// updateBuckets({"3g_sms_mo_success":7,"3g_sms_mo_fail":3});
var stateView = "";
function setStateView(msg){
	if(msg != stateView){
		stateView = msg;
		document.getElementById("state_view").innerHTML = msg;
	}
}

var testMode = "";
function setTestMode(msg){
	if(msg != testMode){
		testMode = msg;
		document.getElementById("test_mode").innerHTML = msg;
	}
}

var errorLog = "";
function setErrorLog(msg){
	if(msg != errorLog){
		errorLog = msg;
		document.getElementById("error_log").textContent = msg;
	}
}

function hideProgress(){
	canvas = document.getElementById("progress_circle");
	canvas.style.visibility="hidden";
}
var progress = 0;
function setProgressPercent(newProgress){
	canvas = document.getElementById("progress_circle");
	canvas.style.visibility="";
	if(progress == newProgress)
		return;
	progress = newProgress;
	centerX = 50;
	centerY = 50;
	radius = 40;
	ctx = canvas.getContext('2d');
	ctx.clearRect(0,0,100,100);
	ctx.beginPath();
	ctx.lineWidth="10";
	ctx.strokeStyle="#000000";
	ctx.arc(centerX,centerY,radius,0-Math.PI/2,2 * Math.PI * (100-progress) / 100 - Math.PI/2);
	ctx.stroke();

}
var currentTest = undefined;
function setCurrentTest(newCurrentTest){
	if(currentTest && document.getElementById(currentTest))
		document.getElementById(currentTest).className = undefined;
	currentTest = newCurrentTest;
	if(currentTest && document.getElementById(currentTest))
		document.getElementById(currentTest).className = "current"
}