import Link from "next/link"
import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

export default function RegisterPage() {
  return (
    <Card className="w-full">
      <CardHeader>
        <CardTitle>Kayıt Ol</CardTitle>
        <CardDescription>
          Fatura yönetim sistemine katılmak için hesap oluşturun.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form>
          <div className="grid w-full items-center gap-4">
            <div className="flex flex-col space-y-1.5">
              <Label htmlFor="fullName">Ad Soyad</Label>
              <Input id="fullName" placeholder="Adınız Soyadınız" />
            </div>
            <div className="flex flex-col space-y-1.5">
              <Label htmlFor="email">E-posta</Label>
              <Input id="email" placeholder="ornek@sirket.com" type="email" />
            </div>
            <div className="flex flex-col space-y-1.5">
              <Label htmlFor="password">Şifre</Label>
              <Input id="password" type="password" />
            </div>
             <div className="flex flex-col space-y-1.5">
              <Label htmlFor="companyName">Şirket Adı</Label>
              <Input id="companyName" placeholder="Şirketinizin Adı" />
            </div>
          </div>
        </form>
      </CardContent>
      <CardFooter className="flex flex-col gap-4">
        <Button className="w-full">Kayıt Ol</Button>
        <div className="text-sm text-center text-muted-foreground">
          Zaten hesabınız var mı?{" "}
          <Link href="/login" className="text-primary hover:underline">
            Giriş Yap
          </Link>
        </div>
      </CardFooter>
    </Card>
  )
}
