import type React from "react"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "./ui/dialog"

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
}

export default function Modal({ isOpen, onClose, title, children }: ModalProps) {
  return (
    <Dialog open={isOpen} onOpenChange={(open) => { if (!open) onClose() }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
        </DialogHeader>
        <div className="text-sm text-muted-foreground leading-relaxed [&_p]:mb-3 [&_ul]:mb-3 [&_ul]:pl-5 [&_li]:mb-1 [&_strong]:text-foreground">
          {children}
        </div>
      </DialogContent>
    </Dialog>
  )
}
