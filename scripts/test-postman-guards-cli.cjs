const http=require('node:http');const {spawn}=require('node:child_process');
const fs=require('node:fs');const os=require('node:os');const path=require('node:path');const assert=require('node:assert/strict');
(async()=>{
 const work=fs.mkdtempSync(path.join(os.tmpdir(),'fluora-postman-guarda-'));let marker=0,protectedCalls=0;
 const server=http.createServer((req,res)=>{
  if(req.url==='/api/v1/publico/__pruebas/aislamiento'){marker++;res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({database:'agenda_db',nonce:'prueba-guarda'}));}
  else{protectedCalls++;res.writeHead(500);res.end('La petición protegida no debería ejecutarse');}
 });
 try{
  await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(18080,'127.0.0.1',resolve);});
  const file=path.resolve(__dirname,'../backend/postman/agenda-web.postman_collection.json');
  const result=await new Promise((resolve,reject)=>{
   const child=spawn('postman',['collection','run',file,'-i','Publicar versión inicial','--env-var','base=http://127.0.0.1:18080','--env-var','entorno=aislado','--env-var','nonce=prueba-guarda','--no-report-events','--reporters','junit','--reporter-junit-export',path.join(work,'guardia.xml')],{stdio:'ignore'});
   child.once('error',reject);child.once('exit',resolve);
  });
  assert.notEqual(result,0,'La corrida debe fallar al no tener entorno aislado');
  assert.equal(marker,1,'Debe comprobar el marcador');assert.equal(protectedCalls,0,'skipRequest asíncrono impide escribir');
  console.log('Guarda asíncrona CLI: 1 marcador, 0 peticiones protegidas, rechazo explícito comprobado.');
 }finally{await new Promise(resolve=>server.close(resolve));fs.rmSync(work,{recursive:true,force:true});}
})().catch(e=>{console.error(e.message);process.exitCode=1;});
