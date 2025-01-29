
echo "Running command 'ant kar'"

ant kar > /dev/null 2>&1;

antReturnCode=$?
 
if [ $antReturnCode -ne 0 ];then
 
    echo "BUILD ERROR: ant kar failed"
    exit 1;
else
 
    echo "SUCCESS: ant kar"
fi



rm -r Docker;

mkdir -p Docker;

cd Docker;

docker cp bpm-ace:/opt/tibco/tibco-karaf-1.0.0-SNAPSHOT/deploy/com.tibco.bpm.karaf.ace.kar .

#rm -r ./repository

unzip com.tibco.bpm.karaf.ace.kar > /dev/null 2>&1

cd ..;

rm -r ./Docker/repository/com/tibco/bpm/karaf/ace/com.tibco.bpm.auth.api
rm -r ./Docker/repository/com/tibco/bpm/karaf/ace/com.tibco.bpm.auth.core
rm -r ./Docker/repository/com/tibco/bpm/karaf/ace/com.tibco.bpm.auth.saml
rm -r ./Docker/repository/com/tibco/bpm/karaf/ace/com.tibco.bpm.karaf.auth-kar


cp -r ./build-artifacts/repository/repository/com/tibco/bpm/karaf/ace/com.tibco.bpm.auth.api ./Docker/repository/com/tibco/bpm/karaf/ace/
cp -r ./build-artifacts/repository/repository/com/tibco/bpm/karaf/ace/com.tibco.bpm.auth.core ./Docker/repository/com/tibco/bpm/karaf/ace/
cp -r ./build-artifacts/repository/repository/com/tibco/bpm/karaf/ace/com.tibco.bpm.auth.saml ./Docker/repository/com/tibco/bpm/karaf/ace/
cp -r ./build-artifacts/repository/repository/com/tibco/bpm/karaf/ace/com.tibco.bpm.karaf.auth-kar ./Docker/repository/com/tibco/bpm/karaf/ace/


cd Docker;

zip -r com.tibco.bpm.karaf.ace.kar ./repository > /dev/null 2>&1

docker cp com.tibco.bpm.karaf.ace.kar bpm-ace:/opt/tibco/tibco-karaf-1.0.0-SNAPSHOT/deploy/

docker restart bpm-ace

cd ..;