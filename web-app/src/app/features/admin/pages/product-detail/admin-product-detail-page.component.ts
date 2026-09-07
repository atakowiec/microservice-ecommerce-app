import {AsyncPipe} from '@angular/common';
import {Component, inject, OnDestroy, OnInit, signal} from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {finalize, shareReplay} from 'rxjs';

import AdminProduct, {
  AdminProductStatus,
} from '../../data-access/product/admin-product.model';
import {
  AdminProductService,
  UpdateAdminProduct,
} from '../../data-access/product/admin-product.service';

type ProductDetailState = 'loading' | 'loaded' | 'not-found' | 'error';
type DisplayedProductImage = {
  id: string;
  fileName: string;
  url: string | null;
};

const requiredTrimmed: ValidatorFn = (
  control: AbstractControl<string>,
): ValidationErrors | null => control.value.trim() ? null : {required: true};

@Component({
  selector: 'app-admin-product-detail-page',
  imports: [AsyncPipe, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-product-detail-page.component.html',
  styleUrl: './admin-product-detail-page.component.scss',
})
export class AdminProductDetailPageComponent implements OnInit, OnDestroy {
  private readonly productService = inject(AdminProductService);
  private readonly route = inject(ActivatedRoute);
  private readonly productId = this.route.snapshot.paramMap.get('productId') ?? '';
  private selectedImage: File | null = null;
  private readonly objectUrls = new Set<string>();
  private imageLoadGeneration = 0;

  readonly state = signal<ProductDetailState>('loading');
  readonly product = signal<AdminProduct | null>(null);
  readonly displayedImages = signal<DisplayedProductImage[]>([]);
  readonly editMode = signal(false);
  readonly submitting = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly productStatuses: readonly AdminProductStatus[] = ['ACTIVE', 'DRAFT', 'ARCHIVED'];
  readonly categories$ = this.productService.findCategories().pipe(
    shareReplay({bufferSize: 1, refCount: true}),
  );
  readonly updateForm = new FormGroup({
    productName: new FormControl('', {
      nonNullable: true,
      validators: [requiredTrimmed],
    }),
    description: new FormControl('', {nonNullable: true}),
    price: new FormControl<number | null>(null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
    categoryId: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    stock: new FormControl<number | null>(null, {
      validators: [
        Validators.required,
        Validators.min(0),
        Validators.pattern(/^\d+$/),
      ],
    }),
    status: new FormControl<AdminProductStatus>('DRAFT', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  ngOnInit(): void {
    this.loadProduct();
  }

  ngOnDestroy(): void {
    this.imageLoadGeneration++;
    this.revokeObjectUrls();
  }

  startEditing(): void {
    const product = this.product();
    if (!product) {
      return;
    }

    this.populateForm(product);
    this.selectedImage = null;
    this.serverError.set(null);
    this.successMessage.set(null);
    this.editMode.set(true);
  }

  cancelUpdate(): void {
    const product = this.product();
    if (product) {
      this.populateForm(product);
    }
    this.selectedImage = null;
    this.serverError.set(null);
    this.editMode.set(false);
  }

  selectImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedImage = input.files?.item(0) ?? null;
  }

  submitUpdate(): void {
    this.serverError.set(null);
    this.updateForm.markAllAsTouched();

    if (this.updateForm.invalid || this.submitting()) {
      return;
    }

    const values = this.updateForm.getRawValue();
    const update: UpdateAdminProduct = {
      productName: values.productName.trim(),
      description: values.description.trim(),
      price: values.price!,
      categoryId: values.categoryId,
      stock: values.stock!,
      status: values.status,
      images: this.selectedImage ? [this.selectedImage] : [],
    };

    this.submitting.set(true);
    this.productService.updateProduct(this.productId, update).pipe(
      finalize(() => this.submitting.set(false)),
    ).subscribe({
      next: product => {
        this.product.set(product);
        this.loadProductImages(product);
        this.populateForm(product);
        this.selectedImage = null;
        this.editMode.set(false);
        this.successMessage.set('Product updated successfully.');
      },
      error: error => {
        if (error.status === 409) {
          this.serverError.set(
            error.error?.message ?? 'A product with this name already exists',
          );
          return;
        }
        if (error.status === 404) {
          this.editMode.set(false);
          this.state.set('not-found');
          return;
        }
        this.serverError.set('The product could not be updated. Please try again.');
      },
    });
  }

  private loadProduct(): void {
    this.state.set('loading');
    this.productService.findProduct(this.productId).subscribe({
      next: product => {
        this.product.set(product);
        this.loadProductImages(product);
        this.populateForm(product);
        this.state.set('loaded');
      },
      error: error => this.state.set(error.status === 404 ? 'not-found' : 'error'),
    });
  }

  private populateForm(product: AdminProduct): void {
    this.updateForm.reset({
      productName: product.productName,
      description: product.description ?? '',
      price: product.price,
      categoryId: product.category.id,
      stock: product.stock,
      status: product.status ?? 'DRAFT',
    });
  }

  private loadProductImages(product: AdminProduct): void {
    const generation = ++this.imageLoadGeneration;
    this.revokeObjectUrls();
    const images = product.images ?? [];
    this.displayedImages.set(images.map(image => ({
      id: image.id,
      fileName: image.fileName,
      url: null,
    })));

    images.forEach((image, imageIndex) => {
      this.productService.findProductImage(product.id, imageIndex).subscribe({
        next: (blob) => {
          if (generation !== this.imageLoadGeneration) {
            return;
          }

          const url = URL.createObjectURL(blob);
          this.objectUrls.add(url);
          this.displayedImages.update(currentImages => currentImages.map(
            (currentImage, currentIndex) => currentIndex === imageIndex
              ? {...currentImage, url}
              : currentImage,
          ));
        },
      });
    });
  }

  private revokeObjectUrls(): void {
    this.objectUrls.forEach(url => URL.revokeObjectURL(url));
    this.objectUrls.clear();
  }
}
