var sendHeartBeat=false;
function heartBeatFunc(){
	if(sendHeartBeat){
		xhr=new XMLHttpRequest();
		if(xhr==undefined) xhr=new ActiveXObject("Microsoft.XMLHTTP");
		xhr.onreadystatechange=function(){
			if (xhr.readyState==4&&xhr.status!=200){
				postMessage(false);
			}
		};
		xhr.open("HEAD", "heartBeat",true);
		xhr.send("");
	}
	sendHeartBeat=true;
	postMessage(true);
}
onmessage=function(e){
	sendHeartBeat=e.data;
}
postMessage(true);
setInterval(heartBeatFunc,2000);