const fs=require('node:fs');
const vm=require('node:vm');

function validateMarketingOrigins(raw) {
  const invalid=()=>new Error('Invalid marketing build origins');
  const origin=value=>{
    if(typeof value!=='string')throw invalid();
    try{const url=new URL(value);
      if(url.protocol!=='https:'||url.username||url.password||url.origin!==value||url.pathname!=='/'||url.search||url.hash||url.hostname==='build.invalid')throw invalid();
      return url;
    }catch{throw invalid();}
  };
  const marketing=origin(raw?.marketingOrigin),app=origin(raw?.appOrigin);
  if(marketing.hostname===app.hostname)throw invalid();
  return Object.freeze({marketingOrigin:marketing.origin,appOrigin:app.origin});
}

function readMarketingOrigins(file) {
  // This evaluates our own public configuration, never environment files or credentials.
  const context=vm.createContext({window:{location:{hostname:'build.invalid',origin:'https://build.invalid'}}},
    {codeGeneration:{strings:false,wasm:false}});
  try{vm.runInContext(fs.readFileSync(file,'utf8'),context,{timeout:1000});}
  catch{throw new Error('Invalid public marketing runtime configuration');}
  return validateMarketingOrigins({marketingOrigin:context.window.__FLUORA_MARKETING_ORIGIN__,appOrigin:context.window.__FLUORA_APP_ORIGIN__});
}

module.exports={readMarketingOrigins,validateMarketingOrigins};
