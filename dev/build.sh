filePath=$(dirname $0)
cd $filePath/..

if [ $# -eq 0 ]; then
 DEPLOY=0
elif [ "$1" == "--deploy" ]; then
 DEPLOY=1 
else
 DEPLOY=0
fi

rm ./out/EC-AzureContainerService.jar
rm ./EC-AzureContainerService.zip

jar cvf ./out/EC-AzureContainerService.jar dsl/ META-INF/ pages/ lib/ htdocs/
zip -r ./EC-AzureContainerService.zip dsl/ META-INF/ pages/ lib/ htdocs/

if [ $DEPLOY -eq 1 ]; then
  echo "Installing plugin ..."
  ectool --server localhost login admin changeme
  ectool installPlugin ./out/EC-AzureContainerService.jar --force 1
  ectool promotePlugin EC-AzureContainerService-1.0.1
fi  