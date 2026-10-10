import pathlib,subprocess,re,json,sys
feature="931c660a49e511ac4c56728679870d5de8138323"
source="15b8ed84ebb1df318bb3ac070a530d7447976b96"
lcov=pathlib.Path(sys.argv[1])
records=lcov.read_text().split("end_of_record")
result={"feature":feature,"source":source,"lcov":str(lcov),"method":"LCOV executable lines and Istanbul branch arms intersected with added lines from git diff -U0; estimate only, not Sonar conditions","files":[]}
whole_diff=subprocess.check_output(["git","diff","-U0",feature,source],text=True)
diffs={}
for part in whole_diff.split("diff --git ")[1:]:
 m=re.search(r"^a/(.+) b/",part)
 if m:diffs[m[1]]=part
for record in records:
 match=re.search(r"SF:(.+)",record)
 if not match or "src/" not in match[1]:continue
 path=match[1][match[1].index("src/"):]
 if path.endswith(".spec.ts"):continue
 diff=diffs.get(path, "")
 changed=set()
 for m in re.finditer(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@",diff):
  start,count=int(m[1]),int(m[2] or 1);changed.update(range(start,start+count))
 da=[(int(a),int(b)) for a,b in re.findall(r"^DA:(\d+),(\d+)",record,re.M) if int(a) in changed]
 br=[(int(a),b,c,d) for a,b,c,d in re.findall(r"^BRDA:(\d+),(\d+),(\d+),([\d-]+)",record,re.M) if int(a) in changed]
 if not da and not br:continue
 result["files"].append({"path":path,"lines":len(da),"coveredLines":sum(b>0 for a,b in da),"branchArms":len(br),"coveredBranchArms":sum(d not in ["0","-"] for a,b,c,d in br),"uncoveredLines":[a for a,b in da if b==0],"uncoveredBranchLines":sorted(set(a for a,b,c,d in br if d in ["0","-"]))})
total={key:sum(f[key] for f in result["files"]) for key in ["lines","coveredLines","branchArms","coveredBranchArms"]}
total["combinedEstimatePercent"]=round(100*(total["coveredLines"]+total["coveredBranchArms"])/(total["lines"]+total["branchArms"]),2)
total["marginAbove90"]=round(total["combinedEstimatePercent"]-90,2)
result["totals"]=total
print(json.dumps(result,indent=2))
