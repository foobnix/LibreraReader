package org.spreadme.pdfgadgets.ui.pdfview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bouncycastle.tsp.TimeStampToken
import org.bouncycastle.util.encoders.Hex
import org.spreadme.pdfgadgets.model.Signature
import org.spreadme.pdfgadgets.resources.R
import org.spreadme.pdfgadgets.ui.common.Dialog
import org.spreadme.pdfgadgets.ui.common.Tipable
import org.spreadme.pdfgadgets.ui.theme.PDFGadgetsTheme
import org.spreadme.common.format
import java.security.cert.X509Certificate

@Composable
fun SignatureDetail(
    signature: Signature,
    enable: MutableState<Boolean> = mutableStateOf(false)
) {
    val enableState = remember { enable }
    if (enableState.value) {
        Dialog(
            onClose = { enable.value = false },
            title = signature.fieldName,
            resizable = true
        ) {
            //Verify Info
            VerifyDetail(signature)
            //Certificate Info
            CertificateDetail(signature.signatureResult.signingCertificate)
            //Timestamp info
            signature.signatureResult.timeStampToken?.let {
                TimeStampDetail(it)
            }
        }
    }
}

@Composable
fun VerifyDetail(signature: Signature) {
    val signatureVerifyUIState = if (signature.signatureResult.verifySignature) {
        if (signature.signatureCoversWholeDocument) {
            SignatureVerifyUIState("签名有效", verifySuccess())
        } else {
            SignatureVerifyUIState("签名未覆盖全文", verifyWarning())
        }
    } else {
        SignatureVerifyUIState("签名无效", verifyError())
    }

    val signatureVerifyUIColor = signatureVerifyUIState.uiColor
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth().height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(signatureVerifyUIColor.backgound)
            .border(
                1.dp,
                signatureVerifyUIColor.border,
                RoundedCornerShape(8.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.Icons.signature_verify),
            contentDescription = "verify result",
            tint = signatureVerifyUIColor.iconColor,
            modifier = Modifier.padding(start = 16.dp).size(16.dp)
        )
        Text(
            signatureVerifyUIState.message,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.subtitle2,
            color = signatureVerifyUIColor.textColor,
        )
    }
}

@Composable
fun CertificateDetail(certificate: X509Certificate) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.surface),
    ) {
        CertificateRow("版本", certificate.version.toString())
        CertificateRow("序列号", Hex.toHexString(certificate.serialNumber.toByteArray()))
        CertificateRow("签名算法", certificate.sigAlgName)
        CertificateRow("主题", certificate.subjectX500Principal.toString())
        CertificateRow("颁发者", certificate.issuerX500Principal.toString())
        CertificateRow("有效起始日期", certificate.notBefore.format())
        CertificateRow("有效截止日期", certificate.notAfter.format())
    }
}

@Composable
fun CertificateRow(
    title: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.weight(0.2f),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                title,
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.caption,
            )
        }
        Box(
            modifier = Modifier.weight(0.8f),
            contentAlignment = Alignment.CenterStart
        ) {
            Tipable(content) {
                Text(
                    content,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.overline,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TimeStampDetail(timeStampToken: TimeStampToken) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(32.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(0.2f),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "时间戳时间：",
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.caption
                )
            }
            Box(
                modifier = Modifier.weight(0.8f),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    timeStampToken.timeStampInfo.genTime.toString(),
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.overline
                )
            }
        }
    }
}

data class SignatureVerifyUIState(
    val message: String,
    val uiColor: SignatureVerifyUIColor
)

data class SignatureVerifyUIColor(
    val backgound: Color,
    val border: Color,
    val iconColor: Color,
    val textColor: Color
)

@Composable
fun verifySuccess(): SignatureVerifyUIColor = SignatureVerifyUIColor(
    PDFGadgetsTheme.extraColors.successBackground,
    PDFGadgetsTheme.extraColors.successBorder,
    PDFGadgetsTheme.extraColors.success,
    PDFGadgetsTheme.extraColors.onSuccess
)

@Composable
fun verifyWarning(): SignatureVerifyUIColor = SignatureVerifyUIColor(
    PDFGadgetsTheme.extraColors.warningBackground,
    PDFGadgetsTheme.extraColors.warningBorder,
    PDFGadgetsTheme.extraColors.warning,
    PDFGadgetsTheme.extraColors.onWarning
)

@Composable
fun verifyError(): SignatureVerifyUIColor = SignatureVerifyUIColor(
    PDFGadgetsTheme.extraColors.errorBackground,
    PDFGadgetsTheme.extraColors.errorBorder,
    PDFGadgetsTheme.extraColors.error,
    PDFGadgetsTheme.extraColors.onError
)