param (
    [string]$tipoPrecio = "spot"  # Puedes pasar "spot" o "pvpc"
)

# Ruta al archivo JSON de tu cuenta de servicio
$credentialFile = "C:\Users\lucas\Desktop\ConsumoHoy\app\consumohoy-firebase-adminsdk-fbsvc-a418ed1fe4.json"

# Leer el archivo JSON
$serviceAccount = Get-Content $credentialFile -Raw | ConvertFrom-Json

# Crear JWT firmado con RS256
function Create-JWT {
    param (
        [string]$email,
        [string]$privateKey
    )

    $iat = [int][double]::Parse((Get-Date -UFormat %s))
    $exp = $iat + 3600

    $header = @{
        alg = "RS256"
        typ = "JWT"
    }

    $payload = @{
        iss = $email
        scope = "https://www.googleapis.com/auth/firebase.messaging"
        aud = "https://oauth2.googleapis.com/token"
        iat = $iat
        exp = $exp
    }

    $headerEncoded  = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes((ConvertTo-Json $header -Compress))) -replace '\+','-' -replace '/','_' -replace '='
    $payloadEncoded = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes((ConvertTo-Json $payload -Compress))) -replace '\+','-' -replace '/','_' -replace '='
    $toSign = "$headerEncoded.$payloadEncoded"

    # Convertir clave PEM a RSA
    $cleanKey = $privateKey -replace '-----.* PRIVATE KEY-----', '' -replace '\s', ''
    $keyBytes = [Convert]::FromBase64String($cleanKey)

    $rsa = [System.Security.Cryptography.RSA]::Create()
    $rsa.ImportPkcs8PrivateKey($keyBytes, [ref]0)
    $signature = $rsa.SignData([System.Text.Encoding]::UTF8.GetBytes($toSign), [System.Security.Cryptography.HashAlgorithmName]::SHA256, [System.Security.Cryptography.RSASignaturePadding]::Pkcs1)

    $signatureEncoded = [Convert]::ToBase64String($signature) -replace '\+','-' -replace '/','_' -replace '='
    return "$toSign.$signatureEncoded"
}

# Crear el JWT firmado
$jwt = Create-JWT -email $serviceAccount.client_email -privateKey $serviceAccount.private_key

# Solicitar el token de acceso OAuth2
$tokenResponse = Invoke-RestMethod -Uri "https://oauth2.googleapis.com/token" -Method Post -ContentType "application/x-www-form-urlencoded" -Body @{
    grant_type = "urn:ietf:params:oauth:grant-type:jwt-bearer"
    assertion  = $jwt
}

$accessToken = $tokenResponse.access_token

# Mensaje personalizado según tipo de precio
$mensaje = switch ($tipoPrecio.ToLower()) {
    "spot" { "Precios SPOT actualizados" }
    "pvpc" { "Precios PVPC actualizados" }
    default { "Precios eléctricos actualizados" }
}

# Crear mensaje de notificación
$notification = @{
    message = @{
        topic = "precios"
        notification = @{
            title = "ConsumoHoy"
            body  = $mensaje
        }
    }
} | ConvertTo-Json -Depth 10

# Enviar la notificación push
$response = Invoke-RestMethod -Uri "https://fcm.googleapis.com/v1/projects/consumohoy/messages:send" `
    -Method Post `
    -Headers @{
        Authorization = "Bearer $accessToken"
        "Content-Type" = "application/json"
    } `
    -Body $notification

# Mostrar respuesta
$response
