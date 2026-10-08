# Proyecto Java de NanoFiles

Implementación del directorio y los nodos del sistema de intercambio de archivos NanoFiles.

## Organización

- [Código fuente](src/es/um/redes/nanoFiles/README.md): aplicaciones, lógica, consola y protocolos TCP/UDP.
- `nf-shared`, `nf-shared1`, `nf-shared2` y `nf-shared3`: carpetas de contenido de ejemplo para los nodos.
- `.project` y `.classpath`: configuración del proyecto para Eclipse.
- `bin`: clases compiladas presentes en la entrega.

## Desarrollo

Importa esta carpeta como proyecto Java en Eclipse.

Los puntos de entrada son `application.Directory` y `application.NanoFiles`, dentro del paquete `es.um.redes.nanoFiles`. El nodo admite una carpeta compartida como argumento; si no se proporciona, utiliza `nf-shared`.

Para ejecutar los JAR ya incluidos, consulta las instrucciones de la [raíz del repositorio](../README.md).
