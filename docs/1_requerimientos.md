# Requerimientos para Aplicación de Control de Gastos

## Funcionalidades Principales

1. **Gestión de Ingresos y Egresos**: Registro detallado de cada transacción, incluyendo:

   - Monto
   - Fecha
   - Categoría (seleccionada de una lista)
   - Subcategoría (relacionada a la categoría principal)
   - Descripción
   - Método de entrada: texto, voz, imagen
   - Archivos adjuntos: Permitir adjuntar archivos si el método de entrada ha sido voz o imagen, y también agregar más archivos si es necesario.

2. **Categorización de Gastos**: Proporcionar una visión clara de los gastos, permitiendo clasificar cada transacción dentro de una categoría principal y, opcionalmente, en una subcategoría específica.

3. **Manejo de Múltiples Cuentas**: Administración de diferentes tipos de cuentas, tales como:

   - Tarjetas de crédito
   - Tarjetas de débito
   - Cuentas bancarias
   - Efectivo (cuenta cash)
   - Moneda: Capacidad de manejar cuentas en diferentes monedas como MXN, USD o Bitcoin.
   - Creación y eliminación de cuentas: El usuario puede crear, eliminar o deshabilitar cuentas según sus necesidades.
   - Tipos de cuenta: Cuentas con características específicas, como cuentas de débito, tarjetas de crédito, préstamos, deudas, o cuentas de nómina con ingresos programados. Esto ayudará a entender la capacidad de pago para planificar gastos futuros.

4. **Cargos Domiciliados**: Registro de cargos recurrentes para identificar y monitorear pagos automáticos, como servicios y suscripciones. Estos cargos deben tener la opción de:

   - Especificar la periodicidad y la fecha de cargo a las cuentas.
   - Confirmar con el usuario cada cargo (los movimientos se generan automáticamente pero el usuario debe aceptarlos a menos que esté en modo automático).
   - Finalizar el cargo según:
     - Una fecha específica.
     - Un número de pagos determinado.
     - Alcanzar un monto total previamente definido.

5. **Gestión Colaborativa de Cuentas**: Permitir que varias personas gestionen una misma cuenta, facilitando que usuarios (esposa, hijo, etc.) registren sus propios gastos.

6. **Importación Masiva de Datos**: Importación de datos mediante archivos Excel, CSV o JSON con la siguiente estructura específica:

   - **tipoMovimiento**: (ingreso o egreso)
   - **monto**: cantidad del movimiento
   - **fecha**: en formato ISO 8601 (e.g., "2024-11-25T15:30:00Z")
   - **categoría**: seleccionada de una lista
   - **subcategoría**: relacionada a la categoría principal
   - **descripción**: opcional
   - **cuenta**: identificador de la cuenta

7. **Registro Simplificado de Gastos e Ingresos**: La aplicación debe permitir el registro de transacciones a través de métodos simples como:

   - Mensajes de texto
   - Fotos de tickets
   - Mensajes de voz
   - Combinación de los métodos anteriores

   La aplicación procesará automáticamente estos registros para identificar la información clave (monto, fecha, categoría) y confirmar la transacción con el usuario.

   Cuando la aplicacion esta ejecutandose en el smartphone pero no hay conexion con el backend, entonces los gastos quedaran en estado pendiente hasta tener conexion de internet con el backend para poder continuar, para este escenario deberiamos tener alguna especie de alertas para avisar al usuario de que hay movimientos pendientes de registrar ahora que ya tenemos internet.

8. **Datos en Nube o Local**: El usuario puede definir si los datos se almacenarán solo localmente o si se subirán a la nube para acceder desde el portal web. Este punto puede ser opcional y se debe discutir más adelante durante el diseño.

9. **Backups en Nube o Local en el Dispositivo**:

   - **Frecuencia de Backups**: El usuario puede definir cada cuánto tiempo se realizarán los backups.
   - **Ubicación de los Backups**: Los backups se pueden guardar localmente o en la nube (e.g., Google Drive, Dropbox).
   - **Formato y Compresión**: Los backups deben estar en formato JSON y comprimidos para optimizar el espacio.
   - **Tipos de Backups**:
     - **Incrementales**: Los backups deben ser incrementales para ahorrar espacio.
     - **Completos**: El usuario puede generar un backup completo en cualquier momento y continuar desde allí.
   - **Archivo Descriptivo**: Cada backup debe tener un archivo descriptivo que incluya la fecha, si es incremental o completo, y detalles sobre su contenido.

10. **Alertas y Presupuestos**:

   - **Límites de Gasto por Categoría**: Posibilidad de definir límites de gasto para cada categoría y recibir alertas cuando se esté acercando a dichos límites.
   - **Planes de Presupuesto**:
     - El usuario puede definir un plan de presupuesto para una o varias categorías y hacer un seguimiento.
     - Cada presupuesto tiene un período definido: semanal, quincenal, mensual, trimestral, semestral o anual.
   - **Manejo de Superávit o Déficit**:
     - El presupuesto se reinicia automáticamente en cada periodo.
     - El usuario puede decidir si el saldo del periodo anterior (superávit o déficit) se transfiere al siguiente periodo o se descarta.
     - Ejemplo: Si el presupuesto es de 1000 pesos quincenales y solo se gastan 800 pesos, se puede optar por trasladar los 200 pesos restantes al siguiente periodo o descartarlos.

11. **Compatibilidad Móvil**: La aplicación debe ser fácil de usar desde dispositivos móviles, asegurando una buena experiencia de usuario. Esto puede lograrse mediante:

   - **App Nativa**: Aplicación instalada directamente en el dispositivo, ofreciendo mejor rendimiento y acceso a funcionalidades específicas del hardware.
   - **Portal Web Móvil**: Un sitio web optimizado para dispositivos móviles, accesible desde cualquier navegador sin necesidad de instalación.

   Dependiendo de las necesidades del usuario, una versión web móvil puede ser suficiente, pero una app nativa puede proporcionar una experiencia más fluida y capacidades avanzadas.

12. **Borrado y Eliminación de Información**: El usuario debe ser capaz de borrar toda la información almacenada. Para ello, se requiere un código de autorización especial, el cual se proporciona tras una verificación de seguridad para evitar borrados accidentales. Este código se obtiene a través de un correo electrónico registrado o mediante autenticación de dos factores, garantizando la legitimidad de la solicitud.

13. **Exportación de Datos**: El usuario debe ser capaz de exportar toda su información en diferentes formatos, como Excel o JSON.

14. **Reportes Financieros**: El usuario tiene acceso a una lista de reportes que le ayudarán a entender sus finanzas y su capacidad de pago. Algunos de los reportes disponibles incluyen:

   - **Reporte de Gastos por Categoría**: Dentro de un periodo específico (mensual, quincenal, semanal, semestral, anual, o personalizado con fecha de inicio y fin). Incluirá gráficos para facilitar la visualización.
   - **Reporte de Presupuestos**: Seguimiento de presupuestos dentro de un periodo específico.
