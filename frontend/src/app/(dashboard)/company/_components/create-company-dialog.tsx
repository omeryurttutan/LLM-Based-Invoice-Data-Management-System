import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import * as z from "zod"
import { useTranslations } from "next-intl"
import { Button } from "@/components/ui/button"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { companyService } from "@/services/company-service"
import { toast } from "sonner"
import { useEffect } from "react"
import { Loader2 } from "lucide-react"

const formSchema = z.object({
    name: z.string().min(2, "Name is required"),
    taxNumber: z.string().length(10, "Tax number must be exactly 10 digits").regex(/^\d+$/, "Must only contain numbers"),
    taxOffice: z.string().optional(),
    address: z.string().optional(),
    city: z.string().optional(),
    district: z.string().optional(),
    postalCode: z.string().optional(),
    phone: z.string().optional(),
    email: z.string().email("Invalid email").optional().or(z.literal('')),
    website: z.string().url("Invalid URL").optional().or(z.literal('')),
    defaultCurrency: z.string().min(1, "Currency is required"),
    invoicePrefix: z.string().optional(),
})

interface CreateCompanyDialogProps {
    open: boolean
    onOpenChange: (open: boolean) => void
    onSuccess: () => void
}

export function CreateCompanyDialog({ open, onOpenChange, onSuccess }: CreateCompanyDialogProps) {
    const t = useTranslations('common.pages.company')

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            name: "",
            taxNumber: "",
            taxOffice: "",
            address: "",
            city: "",
            district: "",
            postalCode: "",
            phone: "",
            email: "",
            website: "",
            defaultCurrency: "TRY",
            invoicePrefix: "",
        },
    })

    useEffect(() => {
        if (open) {
            form.reset()
        }
    }, [open, form])

    const onSubmit = async (values: z.infer<typeof formSchema>) => {
        try {
            await companyService.createCompany(values)
            toast.success(t('messages.saveSuccess') || "Company created successfully")
            onSuccess()
            onOpenChange(false)
        } catch (error) {
            toast.error(t('messages.error') || "An error occurred")
        }
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>{t('addClient') || "Add New Company / Client"}</DialogTitle>
                    <DialogDescription>
                        {t('addDescription') || "Enter the details of the new company you want to manage."}
                    </DialogDescription>
                </DialogHeader>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                        <div className="grid grid-cols-2 gap-4">
                            <FormField
                                control={form.control}
                                name="name"
                                render={({ field }) => (
                                    <FormItem className="col-span-2">
                                        <FormLabel>{t('companyName') || "Company Name"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="taxNumber"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('taxNumber') || "Tax Number"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} maxLength={10} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="taxOffice"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('taxOffice') || "Tax Office"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="defaultCurrency"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('currency') || "Currency"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} placeholder="TRY" />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="email"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('email') || "Email"}</FormLabel>
                                        <FormControl>
                                            <Input type="email" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="phone"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('phone') || "Phone"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="website"
                                render={({ field }) => (
                                    <FormItem className="col-span-2">
                                        <FormLabel>{t('website') || "Website"}</FormLabel>
                                        <FormControl>
                                            <Input type="url" {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="address"
                                render={({ field }) => (
                                    <FormItem className="col-span-2">
                                        <FormLabel>{t('address') || "Address"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="district"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('district') || "District"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="city"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('city') || "City"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="postalCode"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('postalCode') || "Postal Code"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                            <FormField
                                control={form.control}
                                name="invoicePrefix"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('invoicePrefix') || "Invoice Prefix"}</FormLabel>
                                        <FormControl>
                                            <Input {...field} />
                                        </FormControl>
                                        <FormMessage />
                                    </FormItem>
                                )}
                            />
                        </div>
                        <div className="flex justify-end space-x-2 pt-4">
                            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                                {t('cancel') || "Cancel"}
                            </Button>
                            <Button type="submit" disabled={form.formState.isSubmitting}>
                                {form.formState.isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                                {t('createSettings') || "Create"}
                            </Button>
                        </div>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    )
}
