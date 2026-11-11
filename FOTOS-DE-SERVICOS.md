# Fotos dos serviços

Na aba **Serviços** do painel, cada serviço tem o botão **Adicionar foto**; depois do envio, ele passa a dizer **Trocar foto**. O upload aceita JPG/JPEG e PNG de até 5 MB. A imagem fica limitada a 20 megapixels. Apenas o barbeiro autenticado pode enviar; as fotos salvas são servidas publicamente nos cards de serviço.

Por padrão, os arquivos ficam em `./uploads/services/`, relativos ao diretório em que a aplicação inicia. Para produção, defina `BARBERSHOP_UPLOAD_DIR` para um diretório persistente (por exemplo, um volume montado) e faça backup desse diretório. A URL da foto é guardada no banco junto ao serviço; os arquivos antigos são removidos após a substituição ser confirmada.

Fotos configuradas em `src/main/resources/static/js/service-images.js` continuam sendo usadas quando um serviço não tem foto enviada pelo painel.
